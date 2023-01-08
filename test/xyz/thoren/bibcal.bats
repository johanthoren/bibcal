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
    assert_output_matches "^[0-9]+\.[0-9]+\.[0-9]+-?(alpha|beta|rc)?[0-9]?-?(SNAPSHOT)?$"
}

@test "invoking bibcal with argument -l, -L, and -z, plus incorrect arguments" {
    run ./bibcal -l 40.712778 -L -74.006111 -z America/New_York 2101 9 11 9 0
    assert_status 75
    assert_output "ERROR: Year is outside of range 1584 to 2100."
}

@test "invoking bibcal with argument -l, -L, and -z, plus correct arguments" {
    run ./bibcal -l 40.712778 -L -74.006111 -z America/New_York 2021 9 11 9 0
    assert_status 0
    assert_line_matches 0 "4th day of the 6th month"
    assert_line_matches 1 "6021-06-04"
    assert_line_matches 2 "4th of Elul"
    assert_line_matches 3 "5781-06-04"
    assert_line_matches 4 "7"
    assert_line_matches 5 "true"
    assert_line_matches 6 "2021-09-11 09:00:00"
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

@test "invoking bibcal with option -s" {
    run ./bibcal -s
    fail_if output_matches "Sabbath"
}

@test "invoking bibcal with option -s and arguments on Sabbath" {
    run ./bibcal -s -l 40.712778 -L -74.006111 -z America/New_York 2021 9 11 9 0
    assert_status 0
    fail_if output_matches "Sabbath"
}

@test "invoking bibcal with option -s, and arguments on non-Sabbath" {
    run ./bibcal -s -l 40.712778 -L -74.006111 -z America/New_York 2021 9 13 9 0
    assert_status 1
    fail_if output_matches "Sabbath"
}

@test "invoking bibcal with option -s and -v" {
    run ./bibcal -s -v
    assert_output_matches "Sabbath"
}

@test "invoking bibcal with option -s, -v, and arguments on Sabbath" {
    run ./bibcal -s -v -l 40.712778 -L -74.006111 -z America/New_York 2021 9 11 9 0
    assert_status 0
    assert_output_matches "Sabbath"
}

@test "invoking bibcal with option -s, -v, and arguments on non-Sabbath" {
    run ./bibcal -s -v -l 40.712778 -L -74.006111 -z America/New_York 2021 9 13 9 0
    assert_status 1
    assert_output_matches "Sabbath"
}

@test "invoking bibcal with option -d" {
    run ./bibcal -d
    assert_status 0
    assert_line_matches 0 "Date"
    assert_line_matches 1 "ISO date"
    assert_line_matches 2 "Traditional date"
    assert_line_matches 3 "Traditional ISO date"
    assert_line_matches 4 "Day of week"
    assert_output_matches "Local time"
}

@test "invoking bibcal with option -d and 1 argument" {
    run ./bibcal -d 2021
    assert_status 74
}

@test "invoking bibcal with option -d and 2 arguments" {
    run ./bibcal -d 2021 1
    assert_status 74
}

@test "invoking bibcal with option -d and 4 arguments" {
    run ./bibcal -d 2021 1 4 12
    assert_status 0
    assert_line_matches 0 "Date"
    assert_line_matches 1 "ISO date"
    assert_line_matches 2 "Traditional date"
    assert_line_matches 3 "Traditional ISO date"
    assert_line_matches 4 "Day of week"
    assert_line_matches 5 "Local time"
    fail_if output_matches "Sabbath"
    fail_if output_matches "Major"
    fail_if output_matches "Minor"
}

@test "invoking bibcal with options -d, -v, and 4 arguments" {
    run ./bibcal -d -v 2021 1 4 12
    assert_status 0
    assert_line_matches 0 "Date"
    assert_line_matches 1 "ISO date"
    assert_line_matches 2 "Traditional date"
    assert_line_matches 3 "Traditional ISO date"
    assert_line_matches 4 "Day of week"
    assert_line_matches 5 "Sabbath"
    assert_line_matches 6 "Major"
    assert_line_matches 7 "Minor"
    assert_line_matches 8 "Local time"
}

@test "invoking bibcal with options -l, -L, -d, -v, and 4 arguments" {
    run ./bibcal -l 40.712778 -L -74.006111 -z America/New_York -d -v 2021 1 4 12
    assert_status 0
    assert_line_equals 0 "Date                    20th day of the 10th month"
    assert_line_equals 1 "ISO date                6020-10-20"
    assert_line_equals 2 "Traditional date        20th of Tevet"
    assert_line_equals 3 "Traditional ISO date    5781-10-20"
    assert_line_equals 4 "Day of week             2"
    assert_line_equals 5 "Sabbath                 false"
    assert_line_equals 6 "Major feast day         false"
    assert_line_equals 7 "Minor feast day         false"
    assert_line_equals 8 "Local time              2021-01-04 12:00:00"
    assert_line_equals 9 "Start of year           2020-03-24 19:13:00"
    assert_line_equals 10 "Start of month          2020-12-15 16:29:00"
    assert_line_equals 11 "Start of week           2021-01-02 16:40:00"
    assert_line_equals 12 "Start of day            2021-01-03 16:41:00"
    assert_line_equals 13 "End of day              2021-01-04 16:41:59"
    assert_line_equals 14 "End of week             2021-01-09 16:46:59"
    assert_line_equals 15 "End of month            2021-01-13 16:50:59"
    assert_line_equals 16 "End of year             2021-04-12 19:31:59"
    assert_line_equals 17 "Coordinates             40.712778,-74.006111"
    assert_line_equals 18 "Timezone                America/New_York"
}

@test "invoking bibcal with option -d and 8 arguments" {
    run ./bibcal -d 2021 1 1 12 10 0 20 0
    assert_status 69
}

@test "invoking bibcal with option -t" {
    run ./bibcal -t
    assert_status 0
    assert_line_matches 0 "Date"
    assert_line_matches 1 "ISO date"
    assert_line_matches 2 "Traditional date"
    assert_line_matches 3 "Traditional ISO date"
    assert_line_matches 4 "Day of week"
    assert_output_matches "Local time"
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
    run ./bibcal -t 2021 1 4 12
    assert_status 0
    assert_line_matches 0 "Date"
    assert_line_matches 1 "ISO date"
    assert_line_matches 2 "Traditional date"
    assert_line_matches 3 "Traditional ISO date"
    assert_line_matches 4 "Day of week"
    assert_line_matches 5 "Local time"
    fail_if output_matches "Sabbath"
    fail_if output_matches "Major"
    fail_if output_matches "Minor"
}

@test "invoking bibcal with options -t, -v, and 4 arguments" {
    run ./bibcal -t -v 2021 1 4 12
    assert_status 0
    assert_line_matches 0 "Date"
    assert_line_matches 1 "ISO date"
    assert_line_matches 2 "Traditional date"
    assert_line_matches 3 "Traditional ISO date"
    assert_line_matches 4 "Day of week"
    assert_line_matches 5 "Sabbath"
    assert_line_matches 6 "Major"
    assert_line_matches 7 "Minor"
    assert_line_matches 8 "Local time"
}

@test "invoking bibcal with options -l, -L, -t, -v, and 4 arguments" {
    run ./bibcal -l 40.712778 -L -74.006111 -z America/New_York -t -v 2021 1 4 12
    assert_status 0
    assert_line_equals 0 "Date                    20th day of the 10th month"
    assert_line_equals 1 "ISO date                6020-10-20"
    assert_line_equals 2 "Traditional date        20th of Tevet"
    assert_line_equals 3 "Traditional ISO date    5781-10-20"
    assert_line_equals 4 "Day of week             2"
    assert_line_equals 5 "Sabbath                 false"
    assert_line_equals 6 "Major feast day         false"
    assert_line_equals 7 "Minor feast day         false"
    assert_line_equals 8 "Local time              2021-01-04 12:00:00"
    assert_line_equals 9 "Start of year           2020-03-24 19:13:00"
    assert_line_equals 10 "Start of month          2020-12-15 16:29:00"
    assert_line_equals 11 "Start of week           2021-01-02 16:40:00"
    assert_line_equals 12 "Start of day            2021-01-03 16:41:00"
    assert_line_equals 13 "End of day              2021-01-04 16:41:59"
    assert_line_equals 14 "End of week             2021-01-09 16:46:59"
    assert_line_equals 15 "End of month            2021-01-13 16:50:59"
    assert_line_equals 16 "End of year             2021-04-12 19:31:59"
    assert_line_equals 17 "Coordinates             40.712778,-74.006111"
    assert_line_equals 18 "Timezone                America/New_York"
}

@test "invoking bibcal with option -t and 8 arguments" {
    run ./bibcal -t 2021 1 1 12 10 0 20 0
    assert_status 69
}

@test "invoking bibcal with option -d and -D" {
    run ./bibcal -d -D
    assert_status 70
}

@test "invoking bibcal with option -d and -Y" {
    run ./bibcal -d -Y
    assert_status 72
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

@test "invoking bibcal with option -d, -D, and -Y" {
    run ./bibcal -d -D -Y
    assert_status 70
}

@test "invoking bibcal with option -d, -T, and -Y" {
    run ./bibcal -d -T -Y
    assert_status 70
}

@test "invoking bibcal with option -t, -D, and -Y" {
    run ./bibcal -t -D -Y
    assert_status 70
}

@test "invoking bibcal with option -t, -T, and -Y" {
    run ./bibcal -t -T -Y
    assert_status 70
}

@test "invoking bibcal with option -D" {
    run ./bibcal -D
    assert_status 0
    fail_if output_matches ", "
}

@test "invoking bibcal with option -T" {
    run ./bibcal -T
    assert_status 0
    fail_if output_matches ", "
}

@test "invoking bibcal with option -D and -y" {
    run ./bibcal -D -y
    assert_status 0
    assert_output_matches ", "
}

@test "invoking bibcal with option -D and -Y" {
    run ./bibcal -D -Y
    assert_status 0
    assert_output_matches ", "
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

@test "invoking bibcal with option -D and 1 argument" {
    run ./bibcal -D 2021
    assert_status 74
}

@test "invoking bibcal with option -T and 1 argument" {
    run ./bibcal -T 2021
    assert_status 74
}

@test "invoking bibcal with option -D and 2 arguments" {
    run ./bibcal -D 2021 1
    assert_status 74
}

@test "invoking bibcal with option -T and 2 arguments" {
    run ./bibcal -T 2021 1
    assert_status 74
}

@test "invoking bibcal with option -D and 4 arguments" {
    run ./bibcal -D 2021 1 1 12
    assert_status 0
    fail_if output_matches ", "
}

@test "invoking bibcal with option -T and 4 arguments" {
    run ./bibcal -T 2021 1 1 12
    assert_status 0
    fail_if output_matches ", "
}

@test "invoking bibcal with option -D and 8 arguments" {
    run ./bibcal -D 2021 1 1 12 10 0 20 0
    assert_status 69
}

@test "invoking bibcal with option -T and 8 arguments" {
    run ./bibcal -T 2021 1 1 12 10 0 20 0
    assert_status 69
}

@test "invoking bibcal with option -D, -y, and arguments" {
    run ./bibcal -D -y 2021 1 1 12
    assert_status 0
    assert_output_matches ", "
}

@test "invoking bibcal with option -T, -y, and arguments" {
    run ./bibcal -T -y 2021 1 1 12
    assert_status 0
    assert_output_matches ", "
}

@test "invoking bibcal with option -D, -Y, and arguments" {
    run ./bibcal -D -Y 2021 1 1 12
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

@test "invoking bibcal with non-existing option -e" {
    run ./bibcal -e
    assert_status 1
    assert_output_matches "Unknown option:"
}

# This test should catch the issue with octal numbers described in issue #6
# https://github.com/johanthoren/bibcal/issues/6
@test "invoking bibcal 2023 08 01" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal 2023 08 01
    assert_status 0
    assert_line_matches 0 "14th day of the 5th month"
    assert_line_matches -1 "Start of next day"
}

@test "invoking bibcal 2021" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal 2021
    assert_status 0
    assert_line_equals 0 "2021-01-13 1st day of the 11th month"
    assert_line_equals -1 "2021-12-31 4th day of Hanukkah"
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

@test "invoking bibcal 2051" {
    if [ ! "$(uname)" = "Linux" ]; then
        skip "This test only runs on Linux"
    fi
    run ./bibcal 2051
    assert_status 0
    assert_line_equals 1 "2051-01-13 1st day of the 11th month"
    assert_line_equals -1 "2051-12-31 5th day of Hanukkah"
}

### End of main tests ###
