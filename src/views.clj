(ns views
  (:require [nuzzle.hiccup :refer [raw]]
            [nuzzle.util :as util]))

(defn image
  "Two-arity version is ambigious"
  ([src] (image {} src ""))
  ([opts-or-src src-or-alt]
   (if (map? opts-or-src)
     (image opts-or-src src-or-alt "")
     (image {} opts-or-src src-or-alt)))
  ([opts src alt] [:img (merge opts {:src src :alt alt})]))

(defn unordered-list
  ([items] (unordered-list {} items))
  ([opts items]
   (vec (concat [:ul opts] (for [item items] [:li item])))))

(comment (= (unordered-list (list [:p "A"] [:p "B"] [:p "C"]))
            [:ul {} [:li [:p "A"]] [:li [:p "B"]] [:li [:p "C"]]])
         (= (unordered-list {:class "foo"} (list [:p "A"] [:p "B"] [:p "C"]))
            [:ul {:class "foo"} [:li [:p "A"]] [:li [:p "B"]] [:li [:p "C"]]]))

(def os-logo-uri "https://user-images.githubusercontent.com/22163194/171326988-478d1722-b895-4852-a1e4-5689c736b635.svg")

(defn header
  [{:keys [get-site-data]}]
  (let [{:keys [twitter github email]} (get-site-data :meta)]
    [:header
     [:nav [:a {:id "brand" :href "/"} (image os-logo-uri) [:span "stel.codes"]]
      [:ul {:id "social"}
       [:li [:a {:href github} "Github"]]
       [:li [:a {:href twitter} "Twitter"]]
       [:li [:a {:href email} "Email"]]]]]))

(defn footer [] [:footer [:p "Stel Abrego 2021"]])

(defn window
  [title body]
  (let [bars (raw (slurp "svg/bars.svg"))]
    [:section.window
     [:div.top bars [:span.title title] bars]
     [:div.content body]]))

(defn tag-group
  [{:keys [get-site-data tags]}]
  {:pre [(set? tags)]}
  [:p.tags
   (for [tag tags]
     (let [{:keys [uri title]} (get-site-data [:tags tag])]
       [:a {:class "tag" :href uri} title]))])

(defn window-index-item
  [{:keys [uri title subtitle tags] :as webpage}]
  (list [:a {:class "title" :href uri} title]
        (when subtitle [:p.subtitle subtitle])
        (when (not-empty tags) (tag-group webpage))))

(defn truncated-index-window
  "Expects a group index webpage"
  [{:keys [get-site-data index title uri]}]
  (when-not (empty? index)
    (window title
            (list (->> index
                       (map get-site-data)
                       (sort-by :sort)
                       (reverse)
                       (take 5)
                       (map window-index-item)
                       (unordered-list {:class "index-list"}))
                  (when (> (count index) 5) [:a {:class "more-link" :href uri} "more!"])))))

(defn index-window [{:keys [title index get-site-data]}]
  (window title
          (unordered-list {:class "index-list"}
           (->> index (map get-site-data) (map window-index-item)))))

(defn welcome-section []
  [:section.welcome
   (image {:class "avatar"} "https://user-images.githubusercontent.com/22163194/164172131-9086a741-caa7-4811-b5b0-96e3d0f93b7f.png")
   [:span.name "Stel Abrego, Software Developer"]
   [:div.text
    [:p "Hi! I'm a freelance software hacker with a focus on functional design and web technologies."]
    [:p "Check out my projects, learning resources, and blog posts."]
    ;; TODO fix CV link or render this from markdown
    #_[:p "If you're interested in hiring me, here's my CV I also offer virtual tutoring for coding students."]]])

(defn layout
  [{:keys [title id] :as webpage} & content]
  [:html
   [:head
    [:title (if title (str title " | stel.codes") "stel.codes")]
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible", :content "IE=edge"}]
    [:meta {:name "viewport", :content "width=device-width, initial-scale=1.0"}]
    ;; Icons
    [:link {:href "/assets/icons/apple-touch-icon.png", :sizes "180x180", :rel "apple-touch-icon"}]
    [:link {:href "/assets/icons/favicon-32x32.png", :sizes "32x32", :type "image/png", :rel "icon"}]
    [:link {:href "/assets/icons/favicon-16x16.png", :sizes "16x16", :type "image/png", :rel "icon"}]
    [:link {:href "/assets/icons/site.webmanifest", :rel "manifest"}]
    [:link {:color "#5bbad5", :href "/assets/icons/safari-pinned-tab.svg", :rel "mask-icon"}]
    [:link {:href "/assets/icons/favicon.ico", :rel "shortcut icon"}]
    [:link {:href "/assets/css/main.css" :rel "stylesheet"}]
    [:meta {:content "#da532c", :name "msapplication-TileColor"}]
    [:meta {:content "/assets/icons/browserconfig.xml", :name "msapplication-config"}]
    [:meta {:content "#ffffff", :name "theme-color"}]
    ;; Analytics
    (when nil
      [:script
       {:src "https://plausible.io/js/plausible.js", :data-domain "stel.codes", :defer "defer", :async "async"}])]
   [:body (header webpage) [:main (when (= [] id) {:class "home"}) content] (footer)]])

(defn render-generic-webpage
  [{:keys [repo prod source id title subtitle tags header-image render-content] :as webpage}]
  (layout webpage
          (welcome-section)
          (window (util/kebab-case->lower-case (if (> (count id) 1) (nth id (- (count id) 2)) (first id)))
                  [:article (when header-image (image header-image)) [:h1 title]
                   (when subtitle [:p.subtitle subtitle])
                   (when (not-empty tags) (tag-group webpage))
                   (when (or repo prod source)
                     [:div.top-links (when repo [:span "🧙 " [:a {:href repo} "Open Source Code Repo"]])
                      (when prod [:span "🌙 " [:a {:href prod} "Live App Demo"]])
                      (when source [:span "🧑‍🎓 " [:a {:href source} "Find it here!"]])])
                   (render-content)
                   [:div.circles (take 3 (repeat (raw (slurp "svg/circle.svg"))))]])))

(defn render-index-webpage
  [webpage]
  (layout webpage
          (welcome-section)
          (index-window webpage)))

(defn render-homepage
  [{:keys [get-site-data] :as webpage}]
  (layout webpage
          (welcome-section)
          (truncated-index-window (get-site-data [:coding-projects]))
          (truncated-index-window (get-site-data [:educational-media]))
          (truncated-index-window (get-site-data [:blog-posts]))))

(defn render-webpage [{:keys [id] :as webpage}]
  (cond
    (= [] id) (render-homepage webpage)
    (= [:404] id) (layout webpage [:h1 "404 ;-;"])
    (contains? webpage :index) (render-index-webpage webpage)
    :else (render-generic-webpage webpage)))
