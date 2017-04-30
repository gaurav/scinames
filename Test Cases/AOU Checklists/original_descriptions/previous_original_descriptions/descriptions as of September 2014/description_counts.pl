#!/usr/bin/perl -w

use v5.010;

use strict;
use warnings;

use Text::CSV;

my $csv = Text::CSV->new({
    binary => 1
});

my %description_authority;
my %description_source;
my %description_year;
my %count_year;
open my $fh_description_years, "<:encoding(utf-8)", "description_years.csv"
    or die "Could not open 'description_years.csv': $!";

$csv->column_names($csv->getline($fh_description_years));

while(my $row = $csv->getline_hr($fh_description_years)) {
    my $scname = $row->{'scientificName'};
    $description_authority{$scname} = $row->{'authority'};
    # say "Authority: '" . $row->{'authority'} . "'";
    $description_source{$scname} = $row->{'source'};
    $description_year{$scname} = $row->{'year'};
}

close $fh_description_years;

open my $fh_nacc_list, "<:encoding(utf-8)", "NACC_list_species.csv"
    or die "Could not open 'NACC_list_species.csv': $!";

$csv->column_names($csv->getline($fh_nacc_list));

$csv->print(*STDOUT, ["year", "freq_species_described", "cumulative_species_described"]);
say "";

while(my $row = $csv->getline_hr($fh_nacc_list)) {
    my $scname = $row->{'species'}; 

    my $year = int $description_year{$scname};
    $count_year{$year} = 0 unless exists $count_year{$year};
    $count_year{$year}++;
}

my $cumul = 0;
foreach my $year (sort keys %count_year) {
    $cumul += $count_year{$year};

    $csv->print(*STDOUT, [
        $year, $count_year{$year}, $cumul
    ]);
    say "";

}

say STDERR "\nTotal count: $cumul species.";

close($fh_nacc_list);
