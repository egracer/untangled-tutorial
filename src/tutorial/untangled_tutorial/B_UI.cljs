(ns untangled-tutorial.B-UI
  (:require-macros
    [cljs.test :refer [is]])
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            cljsjs.d3
            [devcards.core :as dc :refer-macros [defcard defcard-doc]]))

(defui Widget
  Object
  (render [this]
    (dom/div nil "Hello world")))

(defui WidgetWithHook
  Object
  (componentWillUpdate [this nextprops nextstate] (println "Component will update"))
  (render [this]
    (dom/div nil "Hello world")))

(def widget (om/factory Widget))

(defui WidgetWithProperties
  Object
  (render [this]
    (let [{:keys [name]} (om/props this)]
      (dom/div nil (str "Hello " name)))))

(def prop-widget (om/factory WidgetWithProperties))


(defcard-doc
  "
  # UI

  Om uses <a href=\"https://facebook.github.io/react/index.html\" target=\"_blank\">React</a> underneath.
  The primary mechanism for creating components is the `defui` macro:"
  (dc/mkdn-pprint-source Widget)
  "This macro generates a React Class as a plain javascript class, so it is completely compatible with the
  React ecosystem.

  Notice the use of `Object`. It indicates that the following list of method bodies (like in protocols) are being
  added to the general class. From an OO perspective, this is like saying \"my widget extends Object\". The
  `render` method is the only method you need, but you can also add in your own methods or React lifecycle methods.

  In the author's opinion the render method should be a pure function whenever possible (avoiding component
  local state if at all possible). Making your rendering pure means that if you ever feel the need to write
  tests around how the UI works (say, for acceptance testing) that you can do so very easily. The lack
  of local state means that the code tends to be so simple as to avoid most of the bugs that plague other
  frameworks.

  ## React Lifecycle Methods

  If you wish to provide <a href=\"https://facebook.github.io/react/docs/component-specs.html#lifecycle-methods\"
  target=\"_blank\">lifecycle methods</a>, you can define them under the Object section of the UI:
  "
  (dc/mkdn-pprint-source WidgetWithHook)
  "
  ## Element factory

  In order to render components on the screen you need an element factory.
  You generate a factory with `om/factory`, which will then
  act like a new 'tag' for your DOM:"
  (dc/mkdn-pprint-source widget)
  "Since they are plain React components you can render them in a <a href=\"https://github.com/bhauman/devcards#devcards\"
  target=\"_blank\">devcard</a>, which makes fine tuning them as pure UI dead simple:

  ```
  (defcard widget-card (widget {}))
  ```
  "
  "The resulting card looks like this:"
  )

(defcard widget-card (widget {}))

(defcard-doc
  "Such components are known as \"stateless components\" in Om because they do not expliticly ask for data. Later,
  when we learn about colocated queries, you'll see it is possible for a component to ask for the data it needs in
  a declarative fashion.

  For now, understand that you can give data to a stateless component via a simple edn map, and pull them out of
  `this` using `om/props`:"
  (dc/mkdn-pprint-source WidgetWithProperties)
  (dc/mkdn-pprint-source prop-widget)
  "
  ```
  (defcard props-card (prop-widget {:name \"Sam\"}))
  ```
  ")

(defcard props-card (prop-widget {:name "Sam"}))

(defui Person
  Object
  (render [this]
    (let [{:keys [name]} (om/props this)]
      (dom/li nil name))))

(def person (om/factory Person {:keyfn :name}))

(defui PeopleList
  Object
  (render [this]
    (let [people (om/props this)]
      (dom/ul nil (map person people)))))

(def people-list (om/factory PeopleList))

(defui Root
  Object
  (render [this]
    (let [{:keys [people number]} (om/props this)]
      (dom/div nil
               (dom/span nil (str "My lucky number is " number " and I have the following friends:"))
               (people-list people)))))

(def root (om/factory Root))

(defcard-doc
  "
  ## Composing the UI

  Composing these is pretty straightforward: pull out the bits from props, and pass them on to subcomponents.
  "
  (dc/mkdn-pprint-source Person)
  (dc/mkdn-pprint-source PeopleList)
  (dc/mkdn-pprint-source Root)
  (dc/mkdn-pprint-source people-list)
  (dc/mkdn-pprint-source person)
  (dc/mkdn-pprint-source root)
  "

  ```
  (defcard root-render (root {:number 52 :people [{:name \"Sam\"} {:name \"Joe\"}]}))
  ```

  It is important to note that _this is exactly how the composition of UI Components always happens_, independent of
  whether or not you use the rest of the features of Om. A root component calls the factory functions of subcomponents
  with an edn map as the first argument. That map is accessed using `om/props` on `this` within the subcomponent. Data
  is passed from component to component through `props`.

  You might notice something new here: the `om/factory` function is supplied with an additional map `{:keyfn :name}`.
  The factory function can be optionally supplied with two keywords: `:keyfn` and `:validator`. `:keyfn` produces the
  <a href=\"https://facebook.github.io/react/docs/multiple-components.html\" target=\"_blank\">React key property</a>
  from component props (here it's `:name`), while `:validator`takes a function that asserts the validity of the props received.

  ## Play With It

  At this point (if you have not already) you should play with the code in `B-UI.cljs`. Search for `root-render`
  and then scan backwards to the source. You should try adding an object to the properties (another person),
  and also try playing with editing/adding to the DOM.
  ")

(defcard root-render (root {:number 52 :people [{:name "Sam"} {:name "Joe"}]}))

(defui Root-computed
  Object
  (render [this]
    (let [{:keys [people number b]} (om/props this)
          {:keys [incHandler boolHandler]} (om/get-computed this)]
      (dom/div nil
               ; code pprinter cannot deal with #js on rendering source. Using clj->js instead
               (dom/button (clj->js {:onClick #(boolHandler)}) "Toggle Luck")
               (dom/button (clj->js {:onClick #(incHandler)}) "Increment Number")
               (dom/span nil (str "My " (if b "" "un") "lucky number is " number
                                  " and I have the following friends:"))
               (people-list people)))))

(def root-computed (om/factory Root-computed))

(defcard-doc
  "
  ## Out-of-band Data: Callbacks and such

  In plain React, you store component local state and pass stuff from the parent through props.
  Om is no different, though component-local state is a matter of much debate since you get many advantages from
  having a stateless UI. In React, you also pass your callbacks through props. In Om, we need a slight variation of
  this.

  In Om, a component can have a query that asks the underlying system for data. If you complect callbacks and such
  with this queried data then you run into trouble. So, in general *props have to do with passing data that
  the component requested from a query*.

  As such, Om has an additional mechanism for passing things that were not specifically asked for in a query: Computed
  properties.

  For your Om UI to function properly you must attach computed properties *to* props via the helper function `om/computed`.
  The child can look for these computed properties using `get-computed`.

  (remember about the use of `clj->js`...devcards can't currently render the source of something with reader tags in it.
  You'd normally write `#js { :onClick ...}`).

  "

  (dc/mkdn-pprint-source Root-computed)
  (dc/mkdn-pprint-source root-computed)

  "
  ## Play with It!

  Open B-UI.cljs, search for `passing-callbacks-via-computed`, and you'll find the card shown below. Interact with it
  in your browser, play with the source, and make sure you understand everything we've covered so far.
  ")

(defcard passing-callbacks-via-computed
         (fn [data-atom-from-devcards _]
           (let [prop-data @data-atom-from-devcards
                 sideband-data {:incHandler  (fn [] (swap! data-atom-from-devcards update-in [:number] inc))
                                :boolHandler (fn [] (swap! data-atom-from-devcards update-in [:b] not))}
                 ]
             (root-computed (om/computed prop-data sideband-data))))
         {:number 42 :people [{:name "Sally"}] :b false}
         {:inspect-data true
          :history      true})

(defcard-doc
  "

  ## Stateful Components

  Earlier we stress that your components should be stateless whenever possible. There are a few
  notable exceptions that we have found useful (or even necessary):

  - The Untangled support viewer shows each app state change. User input (each letter they type) can
  be quite tedious to watch in a support viewer. Moving these kinds of interstitial form interactions
  into local state causes little harm, and greatly enhances support. Om automatically hooks up local
  state to input fields.
  - External library integration. We use stateful components to make things like D3 visualizations.

  ### Form inputs

  Om already hooks local state to form elements. So, in fact, you have to override this to *not* use
  component local state. For text controls we'd recommend you leave it this way. For other controls like
  checkboxes it is probably best to override this (demonstrated later).

  ### External Library State (a D3 example)

  Say you want to draw something with D3. D3 has it's own DOM diffing algorithms, and you want to
  make sure React doesn't muck with the DOM. The following component demonstrates1G
  ")

(defn render-squares [component props]
  (let [svg (-> js/d3 (.select (dom/node component)))
        data (clj->js (:squares props))
        selection (-> svg
                      (.selectAll "rect")
                      (.data data (fn [d] (.-id d))))]
    (-> selection
        .enter
        (.append "rect")
        (.style "fill" (fn [d] (.-color d)))
        (.attr "x" "0")
        (.attr "y" "0")
        .transition
        (.attr "x" (fn [d] (.-x d)))
        (.attr "y" (fn [d] (.-y d)))
        (.attr "width" (fn [d] (.-size d)))
        (.attr "height" (fn [d] (.-size d))))
    (-> selection
        .exit
        (.transition)
        (.style "opacity" "0")
        (.remove))
    false))

(defui D3Thing
  Object
  (componentDidMount [this]
    (render-squares this (om/props this)))
  (shouldComponentUpdate [this next-props next-state]
    (let [props (om/-next-props next-props this)]
      (render-squares this props)
      false))
  (render [this]
    (dom/svg #js {:width 100 :height 100 :viewBox "0 0 1000 1000"})))

(def d3-thing (om/factory D3Thing))

(defn random-square []
  {
   :id    (rand-int 10000000)
   :x     (rand-int 900)
   :y     (rand-int 900)
   :size  (+ 50 (rand-int 300))
   :color (case (rand-int 5)
            0 "yellow"
            1 "green"
            2 "orange"
            3 "blue"
            4 "black")})

(defn add-square [state] (swap! state update :squares conj (random-square)))

(defcard sample-d3-component
         (fn [state-atom _]
           (dom/div nil
                    (dom/button #js {:onClick #(add-square state-atom)} "Add Random Square")
                    (dom/button #js {:onClick #(reset! state-atom {:squares []})} "Clear")
                    (d3-thing @state-atom)))
         {:squares []}
         {:inspect-data true})

(defcard-doc
  "

  ## Important Notes and Further Reading

  - Remember to use `#js` (shown as `clj->js` in many examples) to transform attribute maps for passing to DOM elements
  - Use *cljs* maps as input to your own Elements
  - Extract properties with `om/props`. This is the same for stateful (with queries) or stateless components.
  - Add parent-generated things (like callbacks) using `om/computed`.

  TODO: Add links to various docs
  ")
