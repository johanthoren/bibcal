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

Users of Linux distributions using either [RPM](https://en.wikipedia.org/wiki/RPM_Package_Manager) or [DEB](https://en.wikipedia.org/wiki/Deb_(file_format)) can find such files with
[the latest release](https://github.com/johanthoren/bibcal/releases/latest).
These packages have no dependencies and will install cleanly on any such system.

``` sh
$ paru -S bibcal-bin
```

## Usage

The app is run from the command line. Windows users can use either CMD or
PowerShell and simply go to the directory where the file is located (if it's not
already added to the PATH environment variable) and run `./bibcal.exe` with any
options following.

Note to Windows users: double clicking bibcal.exe will do nothing useful.

Running bibcal without any options or arguments will print a list of feast days
in the current gregorian year. Adding a year as an argument will print the feast
days of that year.

Running bibcal with the option `-t` will print a long summary of the current
biblical date.

Running bibcal with the option `-T` will print a short summary of the current
biblical date.

Example:
``` sh
$ bibcal -T
```
Result:

``` sh
18th of Av
```

If you want to see the biblical date of a specific gregorian date, then add
arguments at the end of the command representing year, month, day, hour, minute,
and second (at least 3 positions). 

Example:
``` sh
$ bibcal -x -74.006111 -y 40.712778 -z America/New_York 2011 1 1 9 0
```
Result:
```sh
+-----------------------+----------------------+
| Key                   | Value                |
+-----------------------+----------------------+
| Gregorian time        | 2011-01-01 09:00:00  |
| Traditional year      | 5771                 |
| Alternative year      | 6010                 |
| Biblical month        | 9                    |
| Biblical day of month | 26                   |
| Biblical day of week  | 7                    |
| Sabbath               | true                 |
| Major feast day       | 2nd day of Hanukkah  |
| Minor feast day       | false                |
| Start of year         | 2010-04-14 19:34:00  |
| Start of month        | 2010-12-06 16:28:00  |
| Start of week         | 2010-12-25 16:34:00  |
| Start of day          | 2010-12-31 16:38:00  |
| End of day            | 2011-01-01 16:38:59  |
| End of week           | 2011-01-01 16:38:59  |
| End of month          | 2011-01-04 16:41:59  |
| End of year           | 2011-04-03 19:21:59  |
| Location              | 40.712778,-74.006111 |
| Timezone              | America/New_York     |
| Config file           | None                 |
+-----------------------+----------------------+
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
