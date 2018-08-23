(ns voynich.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [msgpack.core :as msgpack]
            msgpack.clojure-extensions
            [voynich.core :refer :all])
  (:import [java.io DataInputStream]
           [java.util Arrays]
           [com.google.common.io ByteStreams]))

(defn die
  [& args]
  (apply println args)
  (System/exit 1))

(defn- read-password
  [prompt args]
  (let [pw1 (.readPassword (System/console) prompt (into-array args))
        pw2 (.readPassword (System/console) (str "Repeat " prompt) (into-array args))]
    (if (empty? pw1)
      (do
        (println "Empty passwords not allowed")
        (recur prompt args))
      (if (not (Arrays/equals pw1 pw2))
        (do
          (println "Passwords don't match, please try again.")
          (recur prompt args))
        pw1))))

(defn encrypt
  [args]
  (let [opts (cli/parse-opts args [["-c" "--count N" "Set number of passwords required for decryption"
                                    :default 2
                                    :parse-fn #(Integer/parseInt %)]
                                   ["-n" "--passwords N" "Set total number of passwords (will prompt for each)"
                                    :default 4
                                    :parse-fn #(Integer/parseInt %)]
                                   ["-i" "--input FILE" "Input file to encrypt."]
                                   ["-o" "--output FILE" "Output for encrypted file."]
                                   ["-C" "--cipher NAME" "Set cipher name."
                                    :default "AES/GCM/NoPadding"]
                                   ["-k" "--kdf NAME" "Set key derivation algorithm name."
                                    :default "PBKDF2withHmacSHA256"]
                                   ["-I" "--iterations N" "Set key derivation iteration count."
                                    :default 500000]
                                   [nil "--help" "Show this help and exit."]])]
    (when (-> opts :options :help)
      (println "usage: voynich.main encrypt [options]")
      (println)
      (println (:summary opts))
      (System/exit 0))
    (let [{:keys [input output count passwords]} (:options opts)]
      (when (nil? input)
        (die "Option `--input' is required."))
      (when (nil? output)
        (die "Option `--output' is required."))
      (let [pws (for [i (range passwords)] (read-password "Password %d: " [(inc i)]))
            plaintext (with-open [in (io/input-stream input)]
                        (ByteStreams/toByteArray in))
            ciphertext (binding []
                         (encrypt-multi plaintext count pws))]
        (with-open [out (io/output-stream output)]
          (msgpack/pack-stream ciphertext out))))))

(defn decrypt
  [args]
  (let [opts (cli/parse-opts args [["-i" "--input FILE" "Set encrypted input file."]
                                   ["-o" "--output FILE" "Set decrypted output file."]
                                   [nil "--help" "Show this help and exit."]])]
    (when (-> opts :options :help)
      (println "usage: voynich.main decrypt [options]")
      (println)
      (println (:summary opts))
      (System/exit 0))
    (let [{:keys [input output]} (:options opts)]
      (when (nil? input)
        (die "Option `--input' is required."))
      (when (nil? output)
        (die "Option `--output' is required."))
      (let [data (with-open [in (io/input-stream input)]
                   (msgpack/unpack-stream (DataInputStream. in)))
            count (:peer-count data)
            pws (for [i (range count)]
                  (.readPassword (System/console) "Password %d: " (into-array [(inc i)])))
            plaintext (decrypt-multi data pws)]
        (with-open [out (io/output-stream output)]
          (.write out ^"[B" plaintext))))))

(defn -main
  [& args]
  (case (first args)
    "encrypt" (encrypt (rest args))
    "decrypt" (decrypt (rest args))
    ("-h" "--help" "help") (do
                             (println "usage: voynich.main [encrypt|decrypt] [options]")
                             (println "Try `voynich.main encrypt --help' or `voynich.main decrypt --help' for more info."))
    nil (die "Expecting a command.")
    (die "Unknown command:" (str (first args) \.) "Try `voynich.main help'.")))