(defproject artifex "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :plugins [[lein-virgil "0.1.0"]]
  :java-source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [criterium "0.4.3"]]}}
  :jvm-opts ^:replace ["-server" "-Xmx1g"])
