#!/usr/bin/env bats
export ASSERTION_SOURCE="$(pwd)/test/xyz/thoren/bats/assertions"
load "$(pwd)/test/xyz/thoren/bats/assertion-test-helpers"

### Begin main tests ###

@test "invoking bibcal with the invalid option \"--foo\"" {
    run ./bibcal --foo
    assert_status 1
    assert_line_equals 0 "Unknown option: \"--foo\""
}

@test "invoking bibcal with the invalid argument 200" {
    run ./bibcal 200
    assert_status 75
}

@test "invoking bibcal with the invalid argument \"foo\"" {
    run ./bibcal foo
    assert_status 69
}

@test "invoking bibcal with the invalid arguments \"foo\" 1" {
    run ./bibcal foo 1
    assert_status 69
}

# Options -f, -h, and -V should work without a saved configuration file.

@test "invoking bibcal 2021" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal 2021
    assert_status 0
    assert_line_equals 0 "2021-01-13 1st day of the 11th month"
    assert_line_equals -1 "2021-12-31 4th day of Hanukkah"
}

@test "invoking bibcal 2051" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal 2051
    assert_status 0
    assert_line_equals 1 "2051-01-13 1st day of the 11th month"
    assert_line_equals -1 "2051-12-31 5th day of Hanukkah"
}

@test "invoking bibcal 1101" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal 1101
    assert_status 75
    assert_output "ERROR: Year is outside of range 1584 to 2100."
}

@test "invoking bibcal 2101" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal 2101
    assert_status 75
    assert_output "ERROR: Year is outside of range 1584 to 2100."
}

@test "invoking bibcal -h" {
    run ./bibcal -h
    assert_status 0
    assert_line_equals 0 "bibcal: A command-line tool for calculating dates based on the"
    assert_line_equals 1 "        Bible and the 1st Book of Enoch."
}

@test "invoking bibcal -V" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal -V
    assert_status 0
    assert_output_matches "^[0-9]+\.[0-9]+\.[0-9]+-?(SNAPSHOT)?$"
}

@test "invoking bibcal with argument -l, -L, and -z, plus incorrect arguments" {
    run ./bibcal -l 40.712778 -L -74.006111 -z America/New_York 2101 9 11 9 0
    assert_status 75
    assert_output "ERROR: Year is outside of range 1584 to 2100."
}

@test "invoking bibcal with argument -l, -L, and -z, plus correct arguments" {
    run ./bibcal -l 40.712778 -L -74.006111 -z America/New_York 2021 9 11 9 0
    assert_status 0
    assert_line_matches 0 "2021-09-11 09:00:00"
    assert_line_matches 2 "6021-06-04"
    assert_line_matches 4 "5781-06-04"
    assert_line_matches 5 "7"
    assert_line_matches 6 "true"
}

# Save the configuration options in the first command to avoid errors in later
# tests.

@test "invoking bibcal with options -c, -l, -L, and -z" {
    if [ -f ~/.config/bibcal/config.edn ]; then
        skip "Config file already exists"
    fi
    run ./bibcal -c -l 31.7781161 -L 35.233804 -z Asia/Jerusalem
    assert_status 0
    assert_output "The configuration file has been successfully saved."
}

@test "invoking bibcal without any arguments" {
    run ./bibcal
    assert_status 0
}

@test "invoking bibcal with option -t" {
    run ./bibcal -t
    assert_status 0
    assert_line_matches 0 "Gregorian time"
    assert_line_matches 1 "Date"
    assert_line_matches 2 "ISO date"
    assert_line_matches 3 "Traditional date"
    assert_line_matches 4 "Traditional ISO date"
}

@test "invoking bibcal with option -t and 1 argument" {
    run ./bibcal -t 2021
    assert_status 74
}

@test "invoking bibcal with option -t and 2 arguments" {
    run ./bibcal -t 2021 1
    assert_status 74
}
@test "invoking bibcal with option -t and 4 arguments" {
    run ./bibcal -t 2021 1 1 12
    assert_status 0
    assert_line_matches 0 "Gregorian time"
    assert_line_matches 1 "Date"
    assert_line_matches 2 "ISO date"
    assert_line_matches 3 "Traditional date"
    assert_line_matches 4 "Traditional ISO date"
}

@test "invoking bibcal with option -t and 8 arguments" {
    run ./bibcal -t 2021 1 1 12 10 0 20 0
    assert_status 69
}

@test "invoking bibcal with option -t and -T" {
    run ./bibcal -t -T
    assert_status 70
}

@test "invoking bibcal with option -t and -Y" {
    run ./bibcal -t -Y
    assert_status 72
}

@test "invoking bibcal with option -Y" {
    run ./bibcal -Y
    assert_status 72
}

@test "invoking bibcal with option -t, -T, and -Y" {
    run ./bibcal -t -T -Y
    assert_status 70
}

@test "invoking bibcal with option -T" {
    run ./bibcal -T
    assert_status 0
    fail_if output_matches ", "
}

@test "invoking bibcal with option -T and -y" {
    run ./bibcal -T -y
    assert_status 0
    assert_output_matches ", "
}

@test "invoking bibcal with option -T and -Y" {
    run ./bibcal -T -Y
    assert_status 0
    assert_output_matches ", "
}

@test "invoking bibcal with option -T and 1 argument" {
    run ./bibcal -T 2021
    assert_status 74
}

@test "invoking bibcal with option -T and 2 arguments" {
    run ./bibcal -T 2021 1
    assert_status 74
}

@test "invoking bibcal with option -T and 4 arguments" {
    run ./bibcal -T 2021 1 1 12
    assert_status 0
    fail_if output_matches ", "
}

@test "invoking bibcal with option -T and 8 arguments" {
    run ./bibcal -T 2021 1 1 12 10 0 20 0
    assert_status 69
}

@test "invoking bibcal with option -T, -y, and arguments" {
    run ./bibcal -T -y 2021 1 1 12
    assert_status 0
    assert_output_matches ", "
}

@test "invoking bibcal with option -T, -Y, and arguments" {
    run ./bibcal -T -Y 2021 1 1 12
    assert_status 0
    assert_output_matches ", "
}

@test "invoking bibcal with option -vv" {
    run ./bibcal -vv
    assert_status 0
    assert_line_matches 0 DEBUG
}

@test "invoking bibcal with options -vv and argument" {
    run ./bibcal -vv 2021
    assert_status 0
    assert_line_matches 0 DEBUG
}

@test "invoking bibcal with options -vv and arguments" {
    run ./bibcal -vv 2021 1 1 12
    assert_status 0
    assert_line_matches 0 DEBUG
}

### End of main tests ###
