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

$csv->print(*STDOUT, ["scientificName", "authority", "authorityYear"]);

my $count_missing = 0;
while(my $row = $csv->getline_hr($fh_nacc_list)) {
    my $species_name = $row->{'species'};

    if(exists $description_authority{$species_name}) {
        $csv->print(*STDOUT, [
            $species_name, 
            $description_authority{$species_name}, 
            $description_year{$species_name}
        ]);
        say "";
    } else {
        warn "DESCRIPTION MISSING FOR '$species_name'";
        $count_missing++;
    }

}

say STDERR "\n *** DESCRIPTIONS MISSING FOR $count_missing SPECIES ***"
    unless $count_missing == 0;

close($fh_nacc_list);
