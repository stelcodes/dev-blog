(ns codes.stel.dev-blog.views
  (:require [hiccup2.core :refer [html raw]]
            [hiccup.page :refer [html5]]
            [rum.core :refer [render-static-markup]]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [codes.stel.dev-blog.util :as util]))

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

(defn header
  [{:keys [id->info]}]
  (let [{:keys [twitter github email]} (id->info :meta)]
    [:header
     [:nav [:a {:id "brand" :href "/"} (image "https://s3.stel.codes/nixos-logo.png") [:span "stel.codes"]]
      [:ul {:id "social"}
       [:li [:a {:href github} "Github"]]
       [:li [:a {:href twitter} "Twitter"]]
       [:li [:a {:href email} "Email"]]]]]))

(defn footer [] [:footer [:p "Stel Abrego 2021"]])

(defn window
  [title body]
  (let [bars (slurp (io/resource "svg/bars.svg"))]
    [:section.window
     [:div.top bars [:span.title title] bars]
     [:div.content body]]))

(defn tag-group
  [{:keys [id->info tags]}]
  {:pre [(vector? tags)]}
  [:p.tags
   (for [tag tags]
     (let [{:keys [uri title]} (id->info tag)]
       [:a {:class "tag" :href uri} title]))])

(defn window-list-item
  [{:keys [uri title subtitle tags] :as page}]
  (list [:a {:class "title" :href uri} title]
        (when subtitle [:p.subtitle subtitle])
        (when (not-empty tags) (tag-group page))))

(defn home-content-window
  "Expects a group index page"
  [{:keys [id->info index title uri]}]
  (when-not (empty? index)
    (window title
            (list (->> index
                       (map id->info)
                       (sort-by :sort)
                       (reverse)
                       (take 5)
                       (map window-list-item)
                       (unordered-list))
                  (when (> (count index) 5) [:a {:class "more-link" :href uri} "more!"])))))

(defn welcome-section
  []
  [:section.welcome
   (image {:class "avatar"} "https://s3.stel.codes/avatar-small.png")
   [:span.name "Stel Abrego, Software Developer"]
   [:div.text
    [:p "Hi! I'm a freelance software hacker with a focus on functional design and web technologies."]
    [:p "Check out my projects, learning resources, and blog posts."]
    ;; TODO fix CV link or render this from markdown
    [:p "If you're interested in hiring me, here's my CV I also offer virtual tutoring for coding students."]]])

(defn layout
  [{:keys [title id] :as page} & content]
  (html5
   [:html
    [:head [:title (if title (str title " | stel.codes") "stel.codes")] [:meta {:charset "utf-8"}]
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
        {:src "https://plausible.io/js/plausible.js", :data-domain "stel.codes", :defer "defer", :async "async"}])]]
   [:body (header page) [:main (when (= [] id) {:class "home"}) content] (footer)]))

(defn render-generic
  [{:keys [repo prod source id title subtitle tags header-image render-resource] :as page}]
  (layout page
          (welcome-section)
          (window (util/kebab-case->lower-case (if (> (count id) 1) (nth id (- (count id) 2)) (first id)))
                  [:article (when header-image (image header-image)) [:h1 title]
                   (when subtitle [:p.subtitle subtitle])
                   (when (not-empty tags) (tag-group page))
                   (when (or repo prod source)
                     [:div.top-links (when repo [:span "🧙 " [:a {:href repo} "Open Source Code Repo"]])
                      (when prod [:span "🌙 " [:a {:href prod} "Live App Demo"]])
                      (when source [:span "🧑‍🎓 " [:a {:href source} "Find it here!"]])])
                   (render-resource)
                   [:div.circles (take 3 (repeat (slurp "resources/svg/circle.svg")))]])))

(defn render-generic-index
  [{:keys [title index id->info] :as page}]
  {:pre [(vector? index)]}
  (layout
   page
   (list (welcome-section)
         (window title
                 (unordered-list
                  (->> index (map id->info) (map window-list-item)))))))

(defn render-home
  [{:keys [id->info] :as page}]
  (layout
   page
   (list (welcome-section)
         (home-content-window (id->info [:coding-projects]))
         (home-content-window (id->info [:educational-media]))
         (home-content-window (id->info [:blog-posts])))))

(defn render [{:keys [id] :as page}]
  (cond
    (= [] id) (render-home page)
    (= [:404] id) (layout page [:h1 "404 ;-;"])
    (contains? page :index) (render-generic-index page)
    :else (render-generic page)))
