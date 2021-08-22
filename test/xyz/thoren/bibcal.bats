#!/usr/bin/env bats
#
# This function verifies that the string given as argument matches the regex of
# a valid version number.
verify_version_number() {
    grep -P '^(\d{1,}\.\d{1,}\.\d{1,}-?(SNAPSHOT)?)$' <<< "$1"
}

### Begin main tests ###

@test "invoking bibcal without any arguments" {
    run ./bibcal
    [ "$status" -eq 0 ]
}

@test "invoking bibcal -h" {
    run ./bibcal -h
    [ "$status" -eq 0 ]
    [ "${lines[0]}" = "bibcal: A command-line tool for calculating dates based on the" ]
    [ "${lines[1]}" = "        Bible and the 1st Book of Enoch." ]
}

@test "invoking bibcal -V" {
    run ./bibcal -V
    [ "$status" -eq 0 ]
    [ "$(verify_version_number "${lines[0]}")" ]
}

@test "invoking bibcal with option -vv" {
    run ./bibcal -vv
    [ "$status" -eq 0 ]
    [[ "${lines[0]}" =~ DEBUG ]]
}

@test "invoking bibcal with the invalid option \"--foo\"" {
    run ./bibcal --foo
    # This is expected to fail with exit code 1.
    [ "$status" -eq 1 ]
    [ "${lines[0]}" = "Unknown option: \"--foo\"" ]
}

### End of main tests ###
