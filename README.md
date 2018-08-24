# voynich

Multi-party file encryption.

## Installation

Download uberjars [from GitHub](https://github.com/csm/voynich/releases).

## Usage

Encryption arguments:

```
usage: voynich.main encrypt [options]

  -c, --count N       2                     Set number of passwords required for decryption
  -n, --passwords N   4                     Set total number of passwords (will prompt for each)
  -i, --input FILE                          Input file to encrypt.
  -o, --output FILE                         Output for encrypted file.
  -C, --cipher NAME   AES/GCM/NoPadding     Set cipher name.
  -k, --kdf NAME      PBKDF2withHmacSHA256  Set key derivation algorithm name.
  -I, --iterations N  500000                Set key derivation iteration count.
      --help                                Show this help and exit.
```

When encrypting, the program will prompt you `N*2` times
for passwords (each of the N passwords is prompted for twice).
Barring errors, the output file will contain an encrypted version
of the input file, and any C out of N passwords, in any order, can
decrypt the file later.

Decryption arguments:

```
usage: voynich.main decrypt [options]

  -i, --input FILE   Set encrypted input file.
  -o, --output FILE  Set decrypted output file.
      --help         Show this help and exit.
```

Decryption will prompt you for *C* passwords. You can enter any C distinct
passwords from the N total passwords, in any order, to decrypt the file.

## Examples

    $ java -jar voynich-standalone.jar encrypt -i input.txt -o output.enc
    Password 1: aaaaaa
    Repeat Password 1: aaaaaa
    Password 2: oooooo
    Repeat Password 2: oooooo
    Password 3: eeeeee 
    Repeat Password 3: eeeeee
    Password 4: uuuuuu
    Repeat Password 4: uuuuuu
    $ java -jar voynich-standalone.jar decrypt -i output.enc -o output.txt
    Password 1: aaaaaa
    Password 2: oooooo

## License

Copyright © 2018 Casey Marshall

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
