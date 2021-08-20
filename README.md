# bibcal

A command-line calendar based on the Bible and the 1st Book of Enoch.

## System requirements

There are pre-build binaries for recent version of any x86_64 OS listed below:

- Linux
- MacOS
- Windows

The file bibcal-all.jar can be run by java on any OS:

``` sh
$ java -jar /path/to/bibcal-all.jar [options]
```

## Installation

Download [the latest pre-built
binary archive](https://github.com/johanthoren/bibcal/releases/latest), unpack 
it and place it somewhere on your PATH.

For Linux and MacOS users, you may need to make it executable:

``` sh
cd /path/of/bibcal
$ chmod +x bibcal
```

[Arch Linux](https://archlinux.org/) users may install the package `bibcal-bin`
from the [AUR](https://aur.archlinux.org/):

``` sh
$ paru -S bibcal-bin
```

## Usage

The app is run from the command line. Windows users can use either CMD or
PowerShell and simply go to the directory where the file is located (if it's not
already added to the PATH environment variable) and run `./bibcal.exe` with any
options following.

Note to Windows users: double clicking bibcal.exe will do nothing useful.

## Project status

The project is pre-release and will move quickly. Don't expect options to be
stable until a 1.0 release has been tagged.

## Acknowledgements

All calculations are provided by [the luminary library](https://github.com/johanthoren/luminary).

## License

```
ISC License

Copyright (c) 2021, Johan Thoren

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
```
