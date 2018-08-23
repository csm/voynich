(ns voynich.core
  (:require [clojure.math.combinatorics :refer [combinations]]
            [clojure.string :as string])
  (:import [javax.crypto.spec GCMParameterSpec SecretKeySpec PBEKeySpec IvParameterSpec]
           [javax.crypto Cipher SecretKeyFactory SecretKey]
           [java.security SecureRandom]
           [java.security.spec AlgorithmParameterSpec]))

(def random (SecureRandom.))

(defn- random-bytes
  [n]
  (let [b (byte-array n)]
    (.nextBytes random b)
    b))

(defn- password->key
  [password salt kdf-alg kdf-iterations]
  (-> (SecretKeyFactory/getInstance kdf-alg)
      (.generateSecret (PBEKeySpec. password salt kdf-iterations 256))
      (.getEncoded)))

(defn- encrypt-secret
  [secret key cipher-alg]
  (let [iv (random-bytes 16)
        [cipher mode padding] (string/split cipher-alg #"/")
        cipher (doto (Cipher/getInstance cipher-alg)
                 (.init Cipher/ENCRYPT_MODE (SecretKeySpec. key cipher)
                        (if (= (.toLowerCase mode) "gcm")
                          (GCMParameterSpec. 128 iv)
                          (IvParameterSpec. iv))))]
    [iv (.doFinal cipher secret)]))

(defn encrypt-multi
  "Encrypt secret with every ordered combination of N passwords
   from the set of passwords."
  [secret n passwords & {:keys [cipher-alg kdf-alg kdf-iterations]
                         :or {cipher-alg     "AES/GCM/NoPadding"
                              kdf-alg        "PBKDF2withHmacSHA256"
                              kdf-iterations 500000}}]
  (let [combos (combinations passwords n)
        salts (repeatedly (count combos) #(random-bytes 8))
        keys-salt (map (fn [passwords salt]
                         (let [ks (map #(password->key % salt kdf-alg kdf-iterations) passwords)]
                           [(byte-array (reduce #(map bit-xor %1 %2) ks)) salt]))
                       combos salts)
        ciphertexts (map (fn [[key salt]]
                           (let [[iv ct] (encrypt-secret secret key cipher-alg)]
                             {:salt salt :iv iv :ciphertext ct}))
                         keys-salt)]
    {:peer-count  n
     :cipher      cipher-alg
     :kdf         kdf-alg
     :iterations  kdf-iterations
     :ciphertexts (doall ciphertexts)}))

(defn decrypt-multi
  [{:keys [peer-count cipher kdf iterations ciphertexts]} passwords]
  {:pre [(= peer-count (count passwords))]}
  (loop [cts ciphertexts]
    (when (empty? cts)
      (throw (IllegalArgumentException. "could not decrypt ciphertext with any password combination")))
    (let [{:keys [salt iv ciphertext]} (first cts)
          k (byte-array (reduce #(map bit-xor %1 %2)
                                (map #(password->key % salt kdf iterations) passwords)))
          [c m p] (string/split cipher #"/")
          cipher (doto (Cipher/getInstance cipher)
                   (.init Cipher/DECRYPT_MODE
                          ^SecretKey (SecretKeySpec. k c)
                          ^AlgorithmParameterSpec (if (= (string/lower-case m) "gcm")
                                                    (GCMParameterSpec. 128 iv)
                                                    (IvParameterSpec. iv))))
          plaintext (try
                      (.doFinal cipher ciphertext)
                      (catch Throwable _ nil))]
      (if (nil? plaintext)
        (recur (rest cts))
        plaintext))))