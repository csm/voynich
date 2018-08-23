(defproject com.github.csm/voynich "0.1.1"
  :description "Multi-party encryption tool."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [clojure-msgpack "1.2.0"]
                 [com.google.guava/guava "21.0"]]
  :main ^:skip-aot voynich.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["uberjar"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
