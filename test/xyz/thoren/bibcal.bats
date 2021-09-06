#!/usr/bin/env bats
#
# This function verifies that the string given as argument matches the regex of
# a valid version number.
verify_version_number() {
    grep -P '^(\d{1,}\.\d{1,}\.\d{1,}-?(SNAPSHOT)?)$' <<< "$1"
}

### Begin main tests ###

@test "invoking bibcal with the invalid option \"--foo\"" {
    run ./bibcal --foo
    # This is expected to fail with exit code 1.
    [ "$status" -eq 1 ]
    [ "${lines[0]}" = "Unknown option: \"--foo\"" ]
}

@test "invoking bibcal with the invalid argument 200" {
    run ./bibcal 200
    # This is expected to fail with exit code 1.
    [ "$status" -eq 75 ]
}

@test "invoking bibcal with the invalid argument \"foo\"" {
    run ./bibcal foo
    # This is expected to fail with exit code 1.
    [ "$status" -eq 69 ]
}

@test "invoking bibcal with the invalid arguments \"foo\" 1" {
    run ./bibcal foo 1
    # This is expected to fail with exit code 1.
    [ "$status" -eq 69 ]
}

# Options -f, -h, and -V should work without a saved configuration file.

@test "invoking bibcal 2021" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal 2021
    [ "$status" -eq 0 ]
    [ "${lines[0]}" = "2021-01-13 1st day of the 11th month" ]
    [ "${lines[-1]}" = "2021-12-31 4th day of Hanukkah" ]
}

@test "invoking bibcal 2051" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal 2051
    [ "$status" -eq 0 ]
    [ "${lines[1]}" = "2051-01-13 1st day of the 11th month" ]
    [ "${lines[-1]}" = "2051-12-31 5th day of Hanukkah" ]
}

@test "invoking bibcal -h" {
    run ./bibcal -h
    [ "$status" -eq 0 ]
    [ "${lines[0]}" = "bibcal: A command-line tool for calculating dates based on the" ]
    [ "${lines[1]}" = "        Bible and the 1st Book of Enoch." ]
}

@test "invoking bibcal -V" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal -V
    [ "$status" -eq 0 ]
    [ "$(verify_version_number "${lines[0]}")" ]
}

@test "invoking bibcal with argument -l, -L, and -z, plus arguments" {
    run ./bibcal -l 40.712778 -L -74.006111 -z America/New_York 2021 9 11 9 0
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ "2021-09-11 09:00:00" ]]
    [[ "${lines[3]}" =~ "6021-06-04" ]]
    [[ "${lines[4]}" =~ "5781-06-04" ]]
    [[ "${lines[5]}" =~ "7" ]]
    [[ "${lines[6]}" =~ "true" ]]
}

# Save the configuration options in the first command to avoid errors in later
# tests.

@test "invoking bibcal with options -c, -l, -L, and -z" {
    if [ -f ~/.config/bibcal/config.edn ]; then
        skip "Config file already exists"
    fi
    run ./bibcal -c -l 31.7781161 -L 35.233804 -z Asia/Jerusalem
    [ "$status" -eq 0 ]
    [ "${lines[0]}" = "The configuration file has been successfully saved." ]
}

@test "invoking bibcal without any arguments" {
    run ./bibcal
    [ "$status" -eq 0 ]
}

@test "invoking bibcal with option -t" {
    run ./bibcal -t
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ "Gregorian time" ]]
    [[ "${lines[1]}" =~ "Name" ]]
    [[ "${lines[2]}" =~ "Traditional name" ]]
    [[ "${lines[3]}" =~ "ISO date" ]]
    [[ "${lines[4]}" =~ "Traditional ISO date" ]]
}

@test "invoking bibcal with option -t and 1 argument" {
    run ./bibcal -t 2021
    [ "$status" -eq 74 ]
}

@test "invoking bibcal with option -t and 2 arguments" {
    run ./bibcal -t 2021 1
    [ "$status" -eq 74 ]
}
@test "invoking bibcal with option -t and 4 arguments" {
    run ./bibcal -t 2021 1 1 12
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ "Gregorian time" ]]
    [[ "${lines[1]}" =~ "Name" ]]
    [[ "${lines[2]}" =~ "Traditional name" ]]
    [[ "${lines[3]}" =~ "ISO date" ]]
    [[ "${lines[4]}" =~ "Traditional ISO date" ]]
}

@test "invoking bibcal with option -t and 8 arguments" {
    run ./bibcal -t 2021 1 1 12 10 0 20 0
    [ "$status" -eq 69 ]
}

@test "invoking bibcal with option -t and -T" {
    run ./bibcal -t -T
    [ "$status" -eq 70 ]
}

@test "invoking bibcal with option -t and -Y" {
    run ./bibcal -t -Y
    [ "$status" -eq 72 ]
}

@test "invoking bibcal with option -Y" {
    run ./bibcal -Y
    [ "$status" -eq 72 ]
}

@test "invoking bibcal with option -t, -T, and -Y" {
    run ./bibcal -t -T -Y
    [ "$status" -eq 70 ]
}

@test "invoking bibcal with option -T" {
    run ./bibcal -T
    [ "$status" -eq 0 ]
    [[ ! "${lines[0]}" =~ ", " ]]
}

@test "invoking bibcal with option -T and -y" {
    run ./bibcal -T -y
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ ", " ]]
}

@test "invoking bibcal with option -T and -Y" {
    run ./bibcal -T -Y
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ ", " ]]
}

@test "invoking bibcal with option -T and 1 argument" {
    run ./bibcal -T 2021
    [ "$status" -eq 74 ]
}

@test "invoking bibcal with option -T and 2 arguments" {
    run ./bibcal -T 2021 1
    [ "$status" -eq 74 ]
}

@test "invoking bibcal with option -T and 4 arguments" {
    run ./bibcal -T 2021 1 1 12
    [ "$status" -eq 0 ]
    [[ ! "${lines[0]}" =~ ", " ]]
}

@test "invoking bibcal with option -T and 8 arguments" {
    run ./bibcal -T 2021 1 1 12 10 0 20 0
    [ "$status" -eq 69 ]
}

@test "invoking bibcal with option -T, -y, and arguments" {
    run ./bibcal -T -y 2021 1 1 12
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ ", " ]]
}

@test "invoking bibcal with option -T, -Y, and arguments" {
    run ./bibcal -T -Y 2021 1 1 12
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ ", " ]]
}

@test "invoking bibcal with option -vv" {
    run ./bibcal -vv
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ DEBUG ]]
}

@test "invoking bibcal with options -vv and argument" {
    run ./bibcal -vv 2021
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ DEBUG ]]
}

@test "invoking bibcal with options -vv and arguments" {
    run ./bibcal -vv 2021 1 1 12
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ DEBUG ]]
}

### End of main tests ###
