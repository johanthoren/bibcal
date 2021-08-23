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

[Arch Linux](https://archlinux.org/) users may install [the package
bibcal-bin](https://aur.archlinux.org/packages/bibcal-bin/) from the
[AUR](https://aur.archlinux.org/):

``` sh
$ paru -S bibcal-bin
```

## Usage

The app is run from the command line. Windows users can use either CMD or
PowerShell and simply go to the directory where the file is located (if it's not
already added to the PATH environment variable) and run `./bibcal.exe` with any
options following.

Note to Windows users: double clicking bibcal.exe will do nothing useful.

Running bibcal without any options or arguments will print a brief summary of
the current biblical date.

If you want to see the biblical date of a specific gregorian date, then add
arguments at the end of the command representing year, month, day, hour, minute,
and second. 

Example:
``` sh
$ bibcal -x -74.006111 -y 40.712778 -z America/New_York 2001 9 11 9 0

```
Result:
```sh

+------------------------+-----------------------------------------+
| Key                    | Value                                   |
+------------------------+-----------------------------------------+
| Current location       | 40.712778,-74.006111                    |
| Current timezone       | America/New_York                        |
| Month                  | 6                                       |
| Day of month           | 23                                      |
| Day of week            | 3                                       |
| Sabbath                | false                                   |
| Major feast day        | false                                   |
| Minor feast day        | false                                   |
| Start of current day   | 2001-09-10 19:09                        |
| End of current day     | 2001-09-11 19:09                        |
| Start of current week  | 2001-09-08 19:09                        |
| End of current week    | 2001-09-15 19:09                        |
| Start of current month | 2001-08-19 19:08                        |
| End of current month   | 2001-09-17 19:09                        |
| Start of current year  | 2001-03-25 18:03                        |
| End of current year    | 2002-04-13 19:04                        |
+------------------------+-----------------------------------------+
```

Use `bibcal -h` to see a list of options and arguments.

## Configuration file

To save your timezone, latitude and longitude, create a file
`~/.config/bibcal/config.edn`. If you are running Windows, the file would be
`C:\Users\USERNAME\AppData\Roaming\bibcal\config.edn`. In it, save all or some
of the following key-value pairs:

``` edn
{:zone "Asia/Jerusalem"
 :lat 31.7781161
 :lon 35.233804}
```

This file can be generated for you by using the option `-c` together with the
options that you want to save. Example:

``` sh
$ bibcal -c --lat 31.7781161 --lon 35.233804 --zone Asia/Jerusalem
```

## Project status

The project is pre-alpha and will move quickly. Don't expect options or output
to be stable until a 1.0 release has been tagged.

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
