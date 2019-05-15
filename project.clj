(defproject io.lacuna/artifex "0.1.0-alpha1"
  :dependencies [[io.lacuna/bifurcan "0.1.0"]]
  :java-source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [criterium "0.4.3"]
                                  [virgil "0.1.8"]
                                  [org.clojure/test.check "0.10.0-alpha3"]]}}
  :test-selectors {:default #(not
                               (some #{:benchmark :stress}
                                 (cons (:tag %) (keys %))))
                   :benchmark :benchmark
                   :stress :stress
                   :all (constantly true)}
  :jvm-opts ^:replace ["-server"
                       "-Xmx10g"
                       "-ea:io.lacuna..."
                       "-XX:-OmitStackTraceInFastThrow"
                       #_"-XX:+UnlockDiagnosticVMOptions"
                       #_"-XX:+PrintAssembly"
                       #_"-XX:CompileCommand=print,io.lacuna.artifex.Vec::equals"
                       #_"-XX:CompileCommand=dontinline,io.lacuna.artifex.Vec::equals"]

  ;; deployment
  :url "https://github.com/lacuna/artifex"
  :description "a library for geometric data"
  :license {:name "MIT License"}
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :deploy-repositories {"releases"  {:url   "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                                     :creds :gpg}
                        "snapshots" {:url   "https://oss.sonatype.org/content/repositories/snapshots/"
                                     :creds :gpg}}

  ;; Maven properties for the Maven God
  :scm {:url "git@github.com:lacuna/artifex.git"}
  :pom-addition [:developers [:developer
                              [:name "Zach Tellman"]
                              [:url "http://ideolalia.com"]
                              [:email "ztellman@gmail.com"]
                              [:timezone "-8"]]]
  :classifiers {:javadoc {:java-source-paths ^:replace []
                          :source-paths      ^:replace []
                          :resource-paths    ^:replace ["javadoc"]}
                :sources {:java-source-paths ^:replace ["src"]
                          :resource-paths    ^:replace []}})
