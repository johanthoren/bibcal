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

### Windows
Windows users using [the package manager Scoop](https://scoop.sh/) can install bibcal from the [scoop-clojure bucket](https://github.com/littleli/scoop-clojure). 

### Linux
#### Arch Linux
[Arch Linux](https://archlinux.org/) users may install [the package bibcal-bin](https://aur.archlinux.org/packages/bibcal-bin/) 
from the [AUR](https://aur.archlinux.org/).

``` sh
$ paru -S bibcal-bin
```

#### RPM based distributions
Linux distributions using [RPM packages](https://en.wikipedia.org/wiki/RPM_Package_Manager) can find a freshly baked RPM on
[the latest release page](https://github.com/johanthoren/bibcal/releases/latest).

#### DEB based distributions
Linux distributions using [DEB packages](https://en.wikipedia.org/wiki/Deb_(file_format)) can find a newly pressed DEB on
[the latest release page](https://github.com/johanthoren/bibcal/releases/latest).

### All supported operating systems
Download [the latest pre-built binary archive](https://github.com/johanthoren/bibcal/releases/latest), 
unpack it and place it somewhere on your PATH.

For Linux and MacOS users, you may need to make it executable:

``` sh
cd /path/of/bibcal
$ chmod +x bibcal
```

## Usage

The app is run from the command line. 

Windows users can use either CMD or PowerShell and simply go to the directory where the file is located 
(if it's not already added to the PATH environment variable) 
and run `./bibcal.exe` with any options following.

If you installed it using scoop you can run `bibcal` from any PowerShell terminal since it will automatically be added to your PATH.

Note to Windows users: 
double clicking bibcal.exe will do nothing useful.

Running bibcal without any options or arguments will print a list of feast days in the current gregorian year. 
Adding a year as an argument will print the feast days of that year.

Running bibcal with the option `-T` will print a short summary of the current biblical date.

Running bibcal with the option `-t` will print a slightly longer summary of the current biblical date.

Example:
``` sh
$ bibcal -T
```
Result:

``` sh
18th of Av
```

If you want to see the biblical date of a specific gregorian date, 
then add arguments at the end of the command representing 
year, month, day, hour, minute, and second (at least 3 positions).

Example:
``` sh
$ bibcal -l -74.006111 -L 40.712778 -z America/New_York 2011 1 1 9 0
```
Result:
```
Date                    26th day of the 9th month
ISO date                6010-09-26
Traditional date        26th of Kislev
Traditional ISO date    5771-09-26
Day of week             7
Sabbath                 true
Major feast day         2nd day of Hanukkah
Current local time      2011-01-01 09:00:00
Start of next day       2011-01-01 16:01:00
```
Run with option `-v` for a more verbose output, 
including the start and end of day, week, month, and year, 
as well as false values that would otherwise be omitted.
``` sh
$ bibcal -v -l -74.006111 -L 40.712778 -z America/New_York 2011 1 1 9 0
```
Result:
```
Date                    26th day of the 9th month
ISO date                6010-09-26
Traditional date        26th of Kislev
Traditional ISO date    5771-09-26
Day of week             7
Sabbath                 true
Major feast day         2nd day of Hanukkah
Minor feast day         false
Current local time      2011-01-01 09:00:00
Start of year           2010-04-14 09:09:00
Start of month          2010-12-06 15:46:00
Start of week           2010-12-25 15:59:00
Start of day            2010-12-31 15:58:00
End of day              2011-01-01 16:00:59
End of week             2011-01-01 16:00:59
End of month            2011-01-04 16:03:59
End of year             2011-04-03 10:16:59
Coordinates             -74.006111,40.712778
Timezone                America/New_York
Config file             /home/jthoren/.config/bibcal/config.edn
```

Use `bibcal -h` to see a list of options and arguments.

## Configuration file

To save your timezone, latitude and longitude, 
create a file `~/.config/bibcal/config.edn`. 
If you are running Windows, 
the file would be `C:\Users\USERNAME\AppData\Roaming\bibcal\config.edn`. 
In it, save all or some of the following key-value pairs:

``` edn
{:zone "Asia/Jerusalem"
 :lat 31.7781161
 :lon 35.233804}
```

This file can be generated for you by using the option `-c` 
together with the options that you want to save. 
Example:

``` sh
$ bibcal -c --lat 31.7781161 --lon 35.233804 --zone Asia/Jerusalem
```

## Acknowledgements

All calculations are provided by 
[the luminary library](https://github.com/johanthoren/luminary).

## Security

All releases are signed with the following key 
which is in turn signed with 
[my master key, which is published at keybase.io](https://keybase.io/johan_thoren):

``` public-key
-----BEGIN PGP PUBLIC KEY BLOCK-----

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
U4s4wOukEsEKMlCpugj5gS0f
=4XxZ
-----END PGP PUBLIC KEY BLOCK-----
```

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

## Contributor Code of Conduct

This project adheres to No Code of Conduct.  We are all adults.  We accept anyone's contributions.  Nothing else matters.

For more information please visit the [No Code of Conduct](https://github.com/domgetter/NCoC) homepage.
