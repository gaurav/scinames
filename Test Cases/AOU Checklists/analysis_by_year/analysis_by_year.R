#
# ANALYSIS FOR THE AOU PAPER BY YEAR (June 27, 2017)
#
# This analysis file includes instructions for both full analyses as well
# as pre-1982 analyses. Set the FLAG_PRE1982 = T if you would like to
# perform this analysis; otherwise, set FLAG_PRE1982 = 
#
# Changes:
#   - June 22, 2017: Check for Poisson overdispersion
#

# Make sure the taxon concept counts align with the trajectory counts.
# 

getwd()

# Set up Windows fonts
windowsFonts(Calibri=windowsFont("Calibri"))
par(family="Calibri")

#
# There is a convenience setup that controls where the output of this script
# goes. If OUTPUT_SCREEN is set to T, then the output will be displayed in R
# itself. If set to F, the output will be written to a PDF file -- unless
# OUTPUT_PNG is set to T, in which case it will be written to a PNG file. By
# running the script one way and then another, it is straightforward to
# generate every possible output.
# 

FLAG_PRE1982 <- T
OUTPUT_SCREEN <- F
OUTPUT_PNG <- T
    # If OUTPUT_PNG is false, we output as PDF instead.

# overall_cex = 1.7 for paper
# Makes the graph legends better.
overall_cex = 1.7
par(cex = overall_cex)

# In this section, the options above are translated into three functions:
#   - start_export('filename') => start exporting to a PNG or PDF, or do nothing if OUTPUT_SCREEN is set
#       (there is no 'end_export()'; just call dev.off() once you're done)
#   - get_filename('filename') => get a filename for the output file, with the right extension and in
#       the right folder ('pre1982_species' if FLAG_PRE1982, else 'all_species')
#
if(OUTPUT_SCREEN) { start_export <- function(fn, width=1000, height=700) { return(NA) }
} else if(OUTPUT_PNG) { extension <- 'png'; start_export <- function(fn, width=1000, height=700) { png(get_filename(fn), width=width, height=height, units="px") }
} else { extension <- 'pdf'; start_export <- function(fn, width=1500, height=1050) { 
    if(width == 1500 && height == 1050) {
        pdf(get_filename(fn)) 
    } else {
        # Converting width/height to inches at 150dpi
        pdf(get_filename(fn), width=width/100, height=height/100) 
    }
}}

if(FLAG_PRE1982) { get_filename <- function(fn) { return(paste("graphs/pre1982_species/", fn, ".", extension, sep="")); }
} else get_filename <- function(fn) { return(paste("graphs/all_species/", fn, ".", extension, sep="")); }


##################################
#### PART 1: ALL LUMPS/SPLITS ####
##################################

all_splumps <- read.csv("../splumps/list-all.csv")

summary(all_splumps$type)
# lump: 148
# splits: 191

all_name_clusters <- read.csv("../currently_recognized/list-2127.csv")
nrow(all_name_clusters)
# - 2127 name clusters
sum(all_name_clusters$taxon_concept_count)
#  - 2,621 taxon concepts

# The above numbers are after filtering to aou_7_57.csv. Without that filtering
# (i.e. at aou_7_57), we have 2,792 names left

project_stats_all <- read.csv("../project_stats/list-all.csv")
nrow(project_stats_all)
# 66 checklists, including duplicates of aou_5_34 and others
data.frame(project_stats_all$dataset, project_stats_all$binomial_count)
# - aou_5_33: 858
# - aou_5_34: 937
# - aou_6: 1908

#####################################
#### OTHER INTRODUCTORY MATERIAL ####
#####################################

splumps <- read.csv("../splumps/list.csv")
summary(splumps$type)
# lump: 142
# splits: 95

project_stats <- read.csv("../project_stats/list.csv")
nrow(project_stats)
# - 66 (64 checklists + aou_5_34.csv + nacc_5_57.csv)
data.frame(project_stats$dataset, project_stats$binomial_count)

summary(project_stats$binomial_count)
binomial_count_by_year <- tapply(
    project_stats$binomial_count,
    project_stats$year,
    max
)
sort(binomial_count_by_year)
# 862 currently recognized, max 874 in 1956

# How many of the 862 are extralimital?
currently_recognized <- read.csv("../currently_recognized/list.csv")
sum(is.na(currently_recognized$order))
# - 28 extralimitals

# So, non-extralimitally:
sum(!is.na(currently_recognized$order))
# - 834 species

##############################
#### PART 2: LUMPS/SPLITS ####
##############################

# Load splumps
splumps <- read.csv("../splumps/list.csv")

# How many splumps in total?
summary(splumps$type)
# lump: 142
# split: 95

lumps <- splumps[splumps$type == "lump",]
nrow(lumps)
# - 142 lumps

splits <- splumps[splumps$type == "split",]
nrow(splits)
# - 95 splits

# Quick histogram to show relative coverage
hist(splumps$year)
hist(splumps[splumps$type == "lump",]$year, add=T, col="red")

#####################################
#### PART 3: Additions/deletions ####
#####################################

# Note: if we ever actually use these numbers anywhere, 
# remember that this INCLUDES NACC_latest -- so the 
# additions and deletions are inflated here!

# Note: it also includes aou_1.txt, so that's an awful
# lot of additions right there huh.

supplement_counts <- read.csv("../project_stats/list.csv")

sum(supplement_counts$count_added)
# - 1170 added
sum(supplement_counts$count_deleted)
# - 349 deleted
sum(supplement_counts$count_lump)
# - 142 lumps
sum(supplement_counts$count_split)
# - 95 splits

# How many checklists do we have?
data.frame(supplement_counts$dataset, supplement_counts$year)
years <- unique(sort(supplement_counts$year))
length(years)
# 64 checklists = 57 supplements + 7 major editions - 1 (because aou_1_07 and aou_2 both take place in 1895) + 1 NACC_latest
#   - aou_5_34 isn't double-counted, because both files are in the same year.
#   - basically years is just not a good way to analyse this dataset mmkay

# Find out counts per year.
library('zoo')

splumps_by_year <- vector()
lumps_by_year <- vector()
splits_by_year <- vector()

for(year in years) {
    year_str <- toString(year)
    
    num_splump <- nrow(splumps[splumps$year == year,])
    num_lump <- nrow(lumps[lumps$year == year,])
    num_split <- nrow(splits[splits$year == year,])
    
    splumps_by_year[year_str] <- num_lump + num_split
    lumps_by_year[year_str] <- num_lump
    splits_by_year[year_str] <- num_split
}

zoo_splumps <- zoo(splumps_by_year, years)
zoo_lumps <- zoo(lumps_by_year, years)
zoo_splits <- zoo(splits_by_year, years)

length(zoo_splumps)
# - 64
sum(zoo_splumps)
# - 237
length(zoo_lumps)
# - 64
sum(zoo_lumps)
# - 142
length(zoo_splits)
# - 64
sum(zoo_splits)
# - 95

# Can we compensate for the number of recognized species in each case?
summary(supplement_counts$binomial_count)
binomial_count_by_year <- tapply(
    supplement_counts$binomial_count,
    supplement_counts$year,
    max
)
sort(binomial_count_by_year)
# min: 771 in 1886
# max: 874 in 1956
# final: 862 in 2016
zoo_binomial_count_by_year <- zoo(binomial_count_by_year, names(binomial_count_by_year))

# Checklists with zero splumps
splumps_by_year_with_zero <- splumps_by_year[splumps_by_year == 0]
length(splumps_by_year_with_zero)
# 11 checklists, including 1886 and 2017, which shouldn't count
splumps_by_year_with_zero
# - 1886, 1894, 1909, 1912, 1920, 1957, 1983, 1991, 1998, 2009, 2017

#### FIGURE 1a. Bar graph of lumps and splits with cumulative curves ####

library(Hmisc)
start_export('cumul_lumps_and_splits_bargraph', width=1200, height=600)
max_ylim <- max(sum(zoo_lumps), sum(zoo_splits))

zoo_lumps

previous_margins <- par()$mar
par(mfrow=c(1, 1), cex=overall_cex*0.7, mar=c(5, 5, 2, 5)) # Margins

# Add barplot for lumps and splits.
plot(NA,
     ylim=c(0, max(zoo_lumps, zoo_splits)),
     xlim=c(1886, 2016),
     col=2, 
     ylab="Lumps and splits per checklist", 
     xlab="Year"
     # main="Cumulative lumps and splits from 1889 to 2016"
)
rect(
    as.integer(names(zoo_lumps)) - 0.5,
    rep(0, length(zoo_lumps)),
    as.integer(names(zoo_lumps)),
    zoo_lumps,
    col=2
    #,
    #border=2
)
rect(
    as.integer(names(zoo_splits)),
    rep(0, length(zoo_splits)),
    as.integer(names(zoo_splits)) + 0.5,
    zoo_splits,
    col=4
    #,
    #border=4
)
par(new=T)
plot(type="l", cumsum(zoo_lumps), ylim=c(0, max_ylim), col=2, axes=F, xlab=NA, ylab=NA)
axis(side=4)
mtext(side=4, line=3, 'Cumulative lumps and splits', cex=overall_cex*0.7)
par(new=T)
plot(type="l", cumsum(zoo_splits), ylim=c(0, max_ylim), col=4, axes=F, xlab=NA, ylab=NA)
#minor.tick(nx=10, ny=0)

points(pty=1, cumsum(zoo_lumps), ylim=c(0, max_ylim), col=1, axes=F, xlab=NA, ylab=NA)
points(pty=1, cumsum(zoo_splits), ylim=c(0, max_ylim), col=1, axes=F, xlab=NA, ylab=NA)

# Finish
legend("topleft",
       lty=c(1, 1),
       col=c(2, 4),
       legend=c("Lumps", "Splits")
)

dev.off()

# Add cumulative plots
lines(cumsum(zoo_lumps),
     ylim=c(0, max_ylim), 
     col=2, 
     ylab="Number of changes", 
     xlab="Year"
     # main="Cumulative lumps and splits from 1889 to 2016"
)
minor.tick(nx=10, ny=0)
lines(cumsum(zoo_splits), ylim=c(0, max_ylim), col=4, ylab=NA, xlab=NA)

#par(new=T)
#plot(cumsum(zoo_lumps), ylim=c(0, sum(zoo_lumps)), col=2, ylab=NA, xlab=NA, axes=F)
#points(zoo_lumps, col=2, pch=4)
if(0) {
rect(
    as.integer(names(zoo_lumps)) - 0.5,
    rep(0, length(zoo_lumps)),
    as.integer(names(zoo_lumps)),
    zoo_lumps,
    col=2
    # , border=2
)
par(new=T)
plot(cumsum(zoo_splits), ylim=c(0, max_ylim), col=4, ylab=NA, xlab=NA)
rect(
    as.integer(names(zoo_splits)),
    rep(0, length(zoo_splits)),
    as.integer(names(zoo_splits)) + 0.5,
    zoo_splits,
    col=4
    # , border=4
)
#points(zoo_splits, col=4, pch=4)
}

# Annotations!
if(0) {
    # Add lines for all the places where committees had a major change.
    years_committees_changed_bigs <- c(
        1891, 1892, 1894, 1901, 1902, 1920, 1931, 1944, 1954, 1957, 1973, 1976, 1985, 2000
    )
    length(years_committees_changed_bigs)
    abline(v=years_committees_changed_bigs, col="black", lty=2)
}

# Finish
legend("topleft",
       lty=c(1, 1),
       col=c(2, 4),
       legend=c("Lumps", "Splits")
)
dev.off()
par(mfrow=c(1, 1), cex=overall_cex, mar=previous_margins)

#### FIGURE NA: Gap size correlated with post-gap measurement ####

# Do measurements correlate with a lagged measure?
lagged <- c(0, lag(zoo_splumps, k=-1))
lagged
plot(log(zoo_splumps), log(lagged))
cor.test(zoo_splumps, lagged)
# Yes, yes they do (r = 0.409, p < 0.01)
# (I suspect this is just because so many of them are small Poisson-y numbers, 
# so 1 -> 1 happens a lot)

#######################################
#### How important are those gaps? ####
#######################################

start_export('gaps', height=1600, width=1800)
par(mfrow=c(3, 1), cex=overall_cex)

zoo_splumps
zoo_lumps + zoo_splits
sum(zoo_lumps + zoo_splits)

# splumps_by_year
zoo_splumps
splump_years <- as.integer(names(zoo_splumps))
splump_gaps <- c(splump_years, 2017) - c(0, splump_years)
splump_gaps

# The first entry isn't a 1,886 year gap, it should be ignored.
splump_gaps[1] <- NA

# Remove the last one, which is an artifact.
splump_gaps <- splump_gaps[1:(length(splump_gaps) - 1)]

# Okay, we now have splump gaps corresponding to the zoo_splump years, yay!
gap_analysis <- data.frame(
    year = names(zoo_splumps[2:(length(zoo_splumps))]), 
    gap = splump_gaps[2:(length(splump_gaps))],
    splumps = zoo_splumps[2:(length(zoo_splumps))],
    splits = zoo_splits[2:(length(zoo_splits))],
    lumps = zoo_lumps[2:(length(zoo_lumps))]
)
gap_analysis

# We need to plot value ~ gap for each of these three.
# Plot 1. Splumps
plot(gap_analysis$splumps ~ gap_analysis$gap,
    main = "Lumps and splits",
    xlab = "Gap (years)",
    ylab = "Number of changes"
)
gap_model_splumps <- lm(gap_analysis$splumps ~ gap_analysis$gap)
summary(gap_model_splumps)
# - p < 0.0001
# - adjR2 = 0.51
abline(gap_model_splumps, lty=2)

# Plot 2. Lumps
plot(gap_analysis$lumps ~ gap_analysis$gap,
     main = "Lumps only",
     xlab = "Gap (years)",
     ylab = "Number of changes"
)
gap_model_lump <- lm(gap_analysis$lumps ~ gap_analysis$gap)
summary(gap_model_lump)
# - p < 0.0001
# - adjR2 = 0.53
abline(gap_model_lump, lty=2)

# Plot 3. Splumps
plot(gap_analysis$splits ~ gap_analysis$gap,
     main = "Splits only",
     xlab = "Gap (years)",
     ylab = "Number of changes"
)
gap_model_splits <- lm(gap_analysis$splits ~ gap_analysis$gap)
summary(gap_model_splits)
# - p = 0.128
# - R2 = 0.02
abline(gap_model_splits, lty=2)

# Done! Reset par.
dev.off()
par(mfrow=c(1, 1), cex=overall_cex)

###################################################################
#### PART 4: Description rates of currently recognized species ####
###################################################################

latest_aou <- read.csv('../latest_aou_checklist/NACC_list_species_latest.csv')
latest_aou$species_lc <- tolower(latest_aou$species)
LATEST_AOU_COUNT <- length(latest_aou$id)
LATEST_AOU_COUNT
# - 2127

#### Identify when currently recognized species were described ####

original_descs <- read.csv('../original_descriptions/original_descriptions.csv')

nrow(original_descs)
# - 2127
summary(original_descs$year)
# - min: 1758, max: 2011
sum(is.na(original_descs$year))
# - 0
original_descs$species_lc <- tolower(original_descs$species)

latest_aou_with_descriptions <- merge(latest_aou, original_descs, by = "species_lc", all.x = TRUE)
nrow(latest_aou_with_descriptions)
# - 2127

#original_descs[351,]$year
#sum(latest_aou$species_lc == "polioptila caerulea")
#sum(original_descs$species_lc == "polioptila caerulea")

# Any missing?
latest_aou_with_descriptions[which(is.na(latest_aou_with_descriptions$year)),]
# None!

# How many blank years?
sum(is.na(latest_aou_with_descriptions$year))
# = 0
sum((latest_aou_with_descriptions$year == ""))
# = 0

# Divide by decade
description_years_by_decade = tapply(latest_aou_with_descriptions$year, floor(latest_aou_with_descriptions$year / 10) * 10, length)
description_years_by_decade
rev(description_years_by_decade)
cumsum(rev(description_years_by_decade))
cumsum(rev(table(latest_aou_with_descriptions$year)))
# - 2127 in total

# Described since 1950: 14
round(14/2127 * 100, 2)
# - 0.66%

round(101/2127 * 100, 2)
# - 101 since 1900 (4.75%)

round(169/2127 * 100, 2)
# - 169 since 1889 (7.95%) 

round(191/2127 * 100, 2)
# - 191 since 1886 (8.98%) 

# What are the proportions of the species in this study?
name_clusters <- read.csv("../currently_recognized/list.csv")
nrow(name_clusters)
# - 862 name clusters

sum(name_clusters$taxon_concept_count)
# - 1226 taxon concepts

name_clusters$species_lc <- tolower(name_clusters$species)
name_clusters$name_lc <- tolower(name_clusters$name)

# Name clusters after filtering.
name_clusters_recognized <- name_clusters[name_clusters$taxon_concept_count > 0,]
nrow(name_clusters_recognized)
# - 862
name_clusters_not_extralimital <- name_clusters_recognized[!is.na(name_clusters_recognized$order),]
nrow(name_clusters_not_extralimital)
# - 834

# Difference?
nrow(name_clusters_recognized) - nrow(name_clusters_not_extralimital)
# - 28

name_clusters_not_extralimital_with_desc <- merge(name_clusters_not_extralimital, original_descs, by.x = "name_lc", by.y = "species_lc", all.x = TRUE)
nrow(name_clusters_not_extralimital_with_desc)
summary(name_clusters_not_extralimital_with_desc$year)
# - No NAs, whoo.
# name_clusters_not_extralimital_with_desc[which(is.na(name_clusters_not_extralimital_with_desc$year)),]

cumsum(rev(table(name_clusters_not_extralimital_with_desc$year)))
# - 834 in total

# Described since 1950
round(3/834 * 100, 2)
# - 0.36%

round(15/834 * 100, 2)
# - 15 since 1900 (1.80%)

round(24/834 * 100, 2)
# - 24 since 1889 (2.88%) 

round(30/834 * 100, 2)
# - 30 since 1886

round(31/834 * 100, 2)
# - 31 since 1885 (3.72%)

#### Figure X. Species description for the 2127 and 834 species. ####
# TODO: we can extend this curve to 2016!

zoo_years_per_entry_latest_aou <- zoo(names(cumsum(table(latest_aou_with_descriptions$year))))
min(zoo_years_per_entry_latest_aou)

par(cex = overall_cex * 1.2)
start_export('species_description_curves')
plot(NA,
    ylim=c(0, max(cumsum(table(latest_aou_with_descriptions$year)))),
    # ylim=c(0, max(cumsum(table(name_clusters_not_extralimital_with_desc$year)))),
    xlim=c(1758, 2016),
    # main="Species description curve in North American Birds",
    ylab="Currently recognized species",
    xlab="Year"
)

lines(
    cumsum(table(latest_aou_with_descriptions$year)) ~ zoo_years_per_entry_latest_aou,
    col="red",
    lty=3
)

if(0) {
points(
    pch=7,
    cumsum(table(latest_aou_with_descriptions$year)) ~ zoo_years_per_entry_latest_aou,
    col="red"
)
}

zoo_years_per_entry_name_clusters_not_extralimital <- zoo(names(cumsum(table(name_clusters_not_extralimital_with_desc$year))))
lines(
    cumsum(table(name_clusters_not_extralimital_with_desc$year)) ~ zoo_years_per_entry_name_clusters_not_extralimital,
    col="blue",
    lty=1
)

if(0) {
points(
    cumsum(table(name_clusters_not_extralimital_with_desc$year)) ~ zoo_years_per_entry_name_clusters_not_extralimital,
    col="blue",
    pch=7
)
}

legend("topleft",
    c(
        "All currently recognized species (n=2027)",
        "After eliminating species added after 1981 (n=834)"
    ),
    lty=c(3, 1),
    col=c("red", "blue")
)

dev.off()
par(cex = overall_cex)

#############################################
#### PART 5: PER-DECADE LUMPS AND SPLITS ####
#############################################

# Prominent periods of lumping and splitting
final_year <- 2016
offset <- 7
period <- 10

# time periods
lumps_by_decade <- tapply(lumps$type, floor((lumps$year - offset) / period) * period + offset, length)
lumps_by_decade
lumps_by_decade["1957"] = 0
lumps_by_decade["1987"] = 0
lumps_by_decade <- lumps_by_decade[order(names(lumps_by_decade))]
names_lumps_by_decade <- paste(names(lumps_by_decade), "-", as.integer(names(lumps_by_decade)) + period - 1, sep="")
names_lumps_by_decade[1] <- "1889-1896"
names(lumps_by_decade) <- names_lumps_by_decade
lumps_by_year
lumps_by_decade

splits_by_decade <- tapply(splits$type, floor((splits$year - offset) / period) * period + offset, length)
splits_by_decade <- splits_by_decade[order(names(splits_by_decade))]
splits_by_decade
splits_by_decade["1917"] = 0
splits_by_decade["1957"] = 0
splits_by_decade <- splits_by_decade[order(names(splits_by_decade))]
names_splits_by_decade <- paste(names(splits_by_decade), "-", as.integer(names(splits_by_decade)) + period - 1, sep="")
names_splits_by_decade[1] <- "1889-1896"
names(splits_by_decade) <- names_splits_by_decade
splits_by_year
splits_by_decade

###############################################
#### FIGURE 1b: Splits and lumps by decade ####
###############################################

start_export(
    paste("splumps_by_decade", sep=""),
    width=1200, 
    height=800)
par(cex=overall_cex*1.2)

barplot(rbind(lumps_by_decade, splits_by_decade), beside=T,
        col=rep(c(2, 4), length(lumps_by_decade)),
        ylab="Number of lumps or splits",
        xlab="Decade"
)
legend("topleft",
       col=c(2, 4),
       pch=c(15, 15),
       legend=c("Lumps", "Splits")
)

dev.off()
par(cex=overall_cex)

###############################################
#### Where do lumping and splitting spike? ####
###############################################

splumps_by_checklist <- tapply(
    splumps$id,
    splumps$dataset,
    length
)
sort(splumps_by_checklist)

lumps_by_checklist <- tapply(
    lumps$id,
    lumps$dataset,
    length
)
sort(lumps_by_checklist)
table(lumps$year)

splits_by_checklist <- tapply(
    splits$id,
    splits$dataset,
    length
)
sort(splits_by_checklist)
table(splits$dataset)

# What proportion of splits take place after 1982?
sum(splits_by_year)
# - 95 splits
splits_by_year[names(splits_by_year) >= 1982]
sum(splits_by_year[names(splits_by_year) >= 1982])
# - 70 splits
pc_splits_after_1982 <- sum(splits_by_year[names(splits_by_year) >= 1982])/sum(splits_by_year) * 100
round(pc_splits_after_1982, 2)
# - 73.68%
70/95

sum(splits_by_year[names(splits_by_year) >= 1980])
# - 70 splits
70/95
round(100 - pc_splits_after_1982, 2)
# - missing: 26.32%

# Splumps after 1980
splumps_after_1980 <- splumps[splumps$year > 1980,]
nrow(splumps_after_1980)
# 80
summary(splumps_after_1980$reversion_count == 0)
# - 51
51/80
# - 64%

################################
#### PART 6: TAXON CONCEPTS ####
################################

# We don't do any filtering for currently-recognized, so that's the only
# taxon concepts we're interested in. So make sure these come from the
# data reconciliation service!
taxon_concepts <- read.csv("../taxon_concepts/list.csv")
nrow(taxon_concepts)
# - 1226 circumscriptions

length(table(taxon_concepts$name_cluster_id))
# - 862 from name clusters

name_clusters_all <- read.csv("../currently_recognized/list.csv")
nrow(name_clusters_all)
# - 862

# Check for duplicate name clusters
which(table(name_clusters_all$id) > 1)

# name_clusters_all[which(name_clusters_all$id == "fcdf842f-b26d-47c7-a0a7-909db9aedcde"),]

# ANY DUPLICATES?
which(table(name_clusters_all$id) > 1)
# - 0 -- hooray!
# name_clusters[name_clusters$id == "71bcdf15-7b5e-4d52-9689-d7dbcab29147",]

# Okay, thanks to the wonder of SciNames, we already have the counts ...
summary(name_clusters_all$taxon_concept_count)
#   Min. 1st Qu.  Median    Mean 3rd Qu.    Max. 
#  1.000   1.000   1.000   1.422   2.000   6.000

# How many name clusters do we have now? Lemme guess ...
nrow(name_clusters)
# - 862! woo!

# But wait, there's more! Some of these are extralimitals, which don't belong
# to THIS dataset. So:
name_clusters <- name_clusters[!is.na(name_clusters$order),]
nrow(name_clusters)
# - 834! woo!

# How many taxon concepts for the 834 name clusters?
summary(name_clusters$taxon_concept_count)
#  Min. 1st Qu.  Median    Mean 3rd Qu.    Max. 
# 1.000   1.000   1.000   1.391   2.000   6.000 

sum(name_clusters$taxon_concept_count)
# 1160 taxon concepts

sum(is.na(name_clusters$taxon_concept_count))
# - 0 NAs
sum(name_clusters$taxon_concept_count)
# - 1160 taxon concepts
sort(name_clusters$taxon_concept_count)
name_clusters[which.max(name_clusters$taxon_concept_count),]
# Junco hyemalis with *six* taxon concepts? Better recheck.
name_clusters[name_clusters$taxon_concept_count == max(name_clusters$taxon_concept_count),]
name_clusters[name_clusters$taxon_concept_count == max(name_clusters$taxon_concept_count),]$taxon_concept_count

# Why is Junco hyemalis so high?
name_clusters_all[name_clusters_all$taxon_concept_count == 6,]

# ... but we should be able to find it from the other table, too.

# How many taxon concepts per name?
taxon_concepts_per_name <- tapply(
    taxon_concepts$name_cluster_id, 
    factor(taxon_concepts$name_cluster_id),
    length
)
taxon_concepts_per_name
sum(taxon_concepts_per_name)
# - 1226 taxon concepts

# This doesn't line up with the 1160 above, because this is based on the
# 862 clusters that include extralimitals, not the 834 that don't.

# What's the max?
max(taxon_concepts_per_name)

# - Get rid of "(not found in dataset)", which is the highest.
# taxon_concepts_per_name[which.max(taxon_concepts_per_name)] <- NA
taxon_concepts_per_name[which.max(taxon_concepts_per_name)]
# - 6 ()
taxon_concepts[taxon_concepts$name_cluster_id == "ea88d3e5-6fd7-4a7e-9542-45876e752ad5",]
#    - And yes, it's Junco hyemalis!

summary(taxon_concepts_per_name)
#   Min. 1st Qu.  Median    Mean 3rd Qu.    Max. 
#   1.000   1.000   1.000   1.422   2.000   6.000
summary(name_clusters_all$taxon_concept_count)
#   1.000   1.000   1.000   1.422   2.000   6.000

# Note that this is larger than the count we have based on the 834 clusters.
summary(name_clusters$taxon_concept_count)
#    Min. 1st Qu.  Median    Mean 3rd Qu.    Max. 
#   1.000   1.000   1.000   1.391   2.000   6.000 

name_clusters$taxon_concept_count
sum(name_clusters$taxon_concept_count)
# - 1160 taxon concepts

# How many currently recognized species have been corrected more than once?
# (We call this the "correction rate" in the paper.)

total_recognized_name_clusters = nrow(name_clusters)
total_recognized_name_clusters
# - 834 name clusters

sum(name_clusters$taxon_concept_count)
# - 1160 taxon concepts

count_name_clusters_exactly_one <- sum(name_clusters$taxon_concept_count == 1)
count_name_clusters_exactly_one
# - 616 name clusters have never been corrected
stability <- count_name_clusters_exactly_one / total_recognized_name_clusters
round(stability * 100, 2)
# - 73.86%

count_name_clusters_more_than_one <- sum(name_clusters$taxon_concept_count > 1)
count_name_clusters_more_than_one
# - 218 name clusters have ever been corrected
correction_rate <- count_name_clusters_more_than_one / total_recognized_name_clusters
round(correction_rate * 100, 2)
# - 26.14%

count_name_clusters_exactly_two <- sum(name_clusters$taxon_concept_count == 2)
count_name_clusters_exactly_two
# - 133 have been recorrected exactly once
single_correction <- count_name_clusters_exactly_two / total_recognized_name_clusters
round(single_correction * 100, 2)
# - 15.95%

count_name_clusters_more_than_two <- sum(name_clusters$taxon_concept_count > 2)
count_name_clusters_more_than_two
# - 85 name clusters have ever been recorrected
recorrection_rate <- count_name_clusters_more_than_two / total_recognized_name_clusters
round(recorrection_rate * 100, 2)
# - 10.19%

name_clusters_more_than_two <- name_clusters[name_clusters$taxon_concept_count > 2,]
name_clusters_more_than_two$name

# So, total:
616 + 133 + 85
# = 834!

# Let's reload splumps just to make debugging easier.
splumps <- read.csv("../splumps/list.csv")

# Question N. How many lumps or splits have been reverted?
nrow(splumps)
# - 237
summary(splumps$type)
# - lump: 142
# - splits: 95

lumps <- splumps[splumps$type == "lump",]
splits <- splumps[splumps$type == "split",]

nrow(lumps)
# - 142
nrow(splits)
# - 95

nrow(lumps) + nrow(splits)
# - 237 taxonomic corrections

# Perfect reversions, man, I don't know.
(splumps$perfect_reversion_count >= 1)

sum(lumps$perfect_reversion_count >= 1)
# - 27 lumps
perfect_reversion_pc_lumps = sum(lumps$perfect_reversion_count >= 1)/nrow(lumps)
round(perfect_reversion_pc_lumps * 100, 2)
# - 19.01%

# Who are these?
perfectly_reverted_lumps <- lumps[lumps$perfect_reversion_count >= 1,]
perfectly_reverted_lumps
# MOST of these are lumps that WILL be reverted, rather than those that 
# are being reverted, i.e. we're counting the SPLITS, not the LUMPS! Cool.

sum(splits$perfect_reversion_count >= 1)
# - 27 splits
perfect_reversion_pc_splits = sum(splits$perfect_reversion_count >= 1)/nrow(splits)
round(perfect_reversion_pc_splits * 100, 2)
# - 28.42%

sum(splumps$perfect_reversion_count >= 1)
# - 54 splumps
perfect_reversion_pc_splumps = sum(splumps$perfect_reversion_count >= 1)/nrow(splumps)
round(perfect_reversion_pc_splumps * 100, 2)
# - 22.78%

# View(data.frame(lumps$reversions,lumps$reversion_count,lumps$reverts_a_previous_change))

sum(lumps$reversion_count >= 1)
# - 43 lumps
reversion_pc_lumps = sum(lumps$reversion_count >= 1)/nrow(lumps)
round(reversion_pc_lumps * 100, 2)
# - lumps: 30.28%

sum(splits$reversion_count >= 1)
# - 43 splits
reversion_pc_splits = sum(splits$reversion_count >= 1)/nrow(splits)
round(reversion_pc_splits * 100, 2)
# - splits: 45.26%

sum(splumps$reversion_count >= 1)
# - 86 splumps
reversion_pc_splumps = sum(splumps$reversion_count >= 1)/nrow(splumps)
round(reversion_pc_splumps * 100, 2)
# - splumps: 36.29%

100 - round(reversion_pc_splumps * 100, 2)
# - 63.71% have never been changed.

# What if we use 'reverts_a_previous_change' to eliminate double-counting?
lumps <- splumps[splumps$type == "lump",]
splits <- splumps[splumps$type == "split",]

nrow(lumps)
# - 142
nrow(splits)
# - 95

sum(lumps$perfectly_reverts_a_previous_change == "yes")
# - 7 lumps
perfect_reversion_pc_lumps = sum(lumps$perfectly_reverts_a_previous_change == "yes")/nrow(lumps)
round(perfect_reversion_pc_lumps * 100, 2)
# - 4.93%

sum(splits$perfectly_reverts_a_previous_change == "yes")
# - 22 splits
perfect_reversion_pc_splits = sum(splits$perfectly_reverts_a_previous_change == "yes")/nrow(splits)
round(perfect_reversion_pc_splits * 100, 2)
# - 23.16%

sum(splumps$perfectly_reverts_a_previous_change == "yes")
# - 29 splumps
perfect_reversion_pc_splumps = sum(splumps$perfectly_reverts_a_previous_change == "yes")/nrow(splumps)
round(perfect_reversion_pc_splumps * 100, 2)
# - 12.24%

# What proportion of perfectly reverting splumps are splits reverting previous lumps?
summary(splumps[splumps$perfectly_reverts_a_previous_change == "yes",]$type)
22/(22+7)
# 75.9%

# Perfect reversions, man, I don't know.
(splumps$reverts_a_previous_change == "yes")

sum(lumps$reverts_a_previous_change == "yes")
# - 12 lumps
reversion_pc_lumps = sum(lumps$reverts_a_previous_change == "yes")/nrow(lumps)
round(reversion_pc_lumps * 100, 2)
# - lumps: 8.45%

sum(splits$reverts_a_previous_change == "yes")
# - 34 splits
reversion_pc_splits = sum(splits$reverts_a_previous_change == "yes")/nrow(splits)
round(reversion_pc_splits * 100, 2)
# - splits: 35.79%

sum(splumps$reverts_a_previous_change == "yes")
# - 46 splumps
reversion_pc_splumps = sum(splumps$reverts_a_previous_change == "yes")/nrow(splumps)
round(reversion_pc_splumps * 100, 2)
# - splumps: 19.41%

# How much higher than the perfect reversion rate is this?
reversion_pc_splumps - perfect_reversion_pc_splumps
# - 7.17%

100 - round(reversion_pc_splumps * 100, 2)
# - 80.59% have never reverted a previous change.

# What proportion of partially reverting splumps are splits reverting previous lumps?
summary(splumps[splumps$reverts_a_previous_change == "yes",]$type)
34/(12+34)
# 73.91%

# What proportion of lumps are later reverted?
nrow(lumps[lumps$reverts_a_later_change == "yes",])
# - 36 lumps
nrow(lumps[lumps$reverts_a_later_change == "yes",])/nrow(lumps)
# - 25.4%

# What proportion of lumps are later reverted?
nrow(lumps[lumps$perfectly_reverts_a_later_change == "yes",])
# - 22 lumps
nrow(lumps[lumps$perfectly_reverts_a_later_change == "yes",])/nrow(lumps)
# - 15.5%

############################
#### SPLUMPS AFTER 1980 ####
############################

splumps_since_1980 <- splumps[splumps$year >= 1980,]
nrow(splumps_since_1980)
# - 80
splumps_since_1980$year
# - in range
sum(splumps_since_1980$reversion_count >= 1)
# - 29
pc_splits_perfectly_reverting_lumps_since_1980 <- sum(splumps_since_1980$reversion_count >= 1)/nrow(splumps_since_1980) * 100
100-round(pc_splits_perfectly_reverting_lumps_since_1980, 2)
# - 63.75%

#############################
#### CHANGE TRAJECTORIES ####
#############################

# We're still on the 834, yes?
nrow(name_clusters)
# - 834! yup!

trajectories <- table(name_clusters$trajectory_lumps_splits)
sum(trajectories)
# - 834 name clusters

df_trajectories <- data.frame(trajectories)
df_trajectories

traj_never_corrected <- df_trajectories[df_trajectories$Var1 == "",]$Freq
traj_never_corrected
# - 616 never corrected
round(traj_never_corrected/sum(trajectories) * 100, 2)
# - 73.86%
616/834

traj_corrected <- (sum(trajectories) - traj_never_corrected)
traj_corrected
# - 218 corrected
traj_corrected/834
# - 26.14%

# Review
df_trajectories

# first lumped
57 + 8 + 4 + 1 + 1 + 1 + 50 + 3 + 2 + 1
# - 128 lumps first
round(128*100/traj_corrected, 2)
# - 58.72
128/834
# - 15.3%
traj_lump_first <- 128

# Review
df_trajectories

# lump only
57 + 1
58/traj_lump_first
# - 45.3%

# lump -> split -> ...
1 + 50 + 3 + 2
# - 56
56/traj_lump_first
# - 43.75%

# lump -> lump -> ...
8 + 4 + 1 + 1
# - 14
14/traj_lump_first
# 10.93%

# LUMP_ONLY + LUMP_SPLIT + LUMP_LUMP
58 + 56 + 14
# - 128
128 - traj_lump_first

# Review
df_trajectories

# first split: 
3 + 72 + 6 + 1 + 6 + 1 + 1
# - 90 splits first
90/traj_corrected
# - 41.28%
90/834
# 10.79
traj_split_first <- 90

# Review
df_trajectories

# split only
3 + 72
# - 75 splits only
75/traj_split_first
# 83.33%

# Review
df_trajectories

# split -> lump -> ...
# (I classifed the split -> lump|split -> split as a split->lump)
6 + 1 + 6 + 1 + 1
# - 15 
15/traj_split_first
# - 16.67%

# split -> split -> ...
# (I classifed the split -> lump|split -> split as a split->lump)
0

# SPLIT ONLY + SPLIT_LUMP + SPLIT_SPLIT
75 + 15 + 0
# = 90
90 - traj_split_first

# Review
df_trajectories

# Changes not fully represented in two steps (i.e. something -> something -> ...)
4 + 1 + 1 + 3 + 1 + 6 + 1 + 1
# - 18
18/834
# 2.16%

# Changes made once and then not repeated
# (LUMP_ONLY + SPLIT_ONLY)
58 + 75
# - 133, exactly what we got before
(58 + 75)/834
# - 15.95%

# Changes made more than once (recorrection)
# (LUMP_SPLIT + SPLIT_LUMP + LUMP_LUMP + SPLIT_SPLIT)
56 + 15 + 14 + 0
# - 85, exactly what we got before
85/834
# - 10.19%

if(0) {
    # DEBUGGING CODE

    # Okay, so, here are the clusters with more than two circumscriptions:
    name_clusters_more_than_two$name
    
    # And here are the ones with the trajectories with more than one step.
    df_trajectories
    
    name_clusters$trajectory_str <- as.character(name_clusters$trajectory_lumps_splits)
    
    nrow(which(name_clusters$trajectory != ""))
    name_clusters_traj_more_than_one <- name_clusters[which(name_clusters$trajectory_str != "" & name_clusters$trajectory_str != "added|split" & name_clusters$trajectory_str != "rename|split" & name_clusters$trajectory_str != "lump" & name_clusters$trajectory_str != "split" & name_clusters$trajectory_str != "split|deleted"),]
    nrow(name_clusters_traj_more_than_one)
    
    as.character(name_clusters_traj_more_than_one$name)
    
    length(as.character(name_clusters_traj_more_than_one$name))
    length(as.character(name_clusters_more_than_two$name))
    
    setdiff(as.character(name_clusters_more_than_two$name), as.character(name_clusters_traj_more_than_one$name))
    # "Junco insularis"         "Aphelocoma coerulescens" "Empidonax wrightii"  
    
    # Okay! So! What are their trajectories?
    name_clusters[which(as.character(name_clusters$name) == "Junco insularis"),]$trajectory
    name_clusters[which(as.character(name_clusters$name) == "Aphelocoma coerulescens"),]$taxon_concepts
    name_clusters[which(as.character(name_clusters$name) == "Empidonax wrightii"),]$taxon_concepts
}

##############################################################
#### How many lumps and splits revert earlier reversions? ####
##############################################################

if(0) {
    lumps_not_reverting_anything <- lumps[lumps$reversion_count == 0,]
    nrow(lumps_not_reverting_anything)
    # - 75 lumps
    
    # Proportion of all lumps
    round(nrow(lumps_not_reverting_anything)/nrow(lumps) * 100, 2)
    # - 65.22%
    
    lumps_with_reversions <- lumps[lumps$reversion_count > 0,]
    nrow(lumps_with_reversions)
    # - 40 lumps
    
    # Of these 39 lumps, what proportion of them are reverting a previous split?
    lumps_with_reversions_reverting_previous <- lumps_with_reversions[lumps_with_reversions$reverts_a_previous_change == "yes",]
    nrow(lumps_with_reversions_reverting_previous)
    # - 10 lumps
    
    # Proportion of all lumps
    round(nrow(lumps_with_reversions_reverting_previous)/nrow(lumps) * 100, 2)
    # - 8.7%
    
    # Proportion of lumps that have not yet been reverted
    count_lumps_never_reverted <- 
        nrow(lumps_not_reverting_anything) +
        nrow(lumps_with_reversions[lumps_with_reversions$reverts_all_previous_changes == "yes",])
    count_lumps_never_reverted
    # - 81
    round(count_lumps_never_reverted/nrow(lumps) * 100, 2)
    # - 70.43%
    
    splits_not_reverting_anything <- splits[splits$reversion_count == 0,]
    nrow(splits_not_reverting_anything)
    # - 47 splits
    
    # Proportion of all splits
    round(nrow(splits_not_reverting_anything)/nrow(splits) * 100, 2)
    # - 55.95%
    
    splits_with_reversions <- splits[splits$reversion_count > 0,]
    nrow(splits_with_reversions)
    # - 37 splits
    
    # Of these 36 splits, what proportion of them are reverting a previous lump?
    splits_with_reversions_reverting_previous <- splits_with_reversions[splits_with_reversions$reverts_a_previous_change == "yes",]
    nrow(splits_with_reversions_reverting_previous)
    # - 31 splits
    
    # Proportion of splits that have not yet been reverted
    count_splits_never_reverted <- 
        nrow(splits_not_reverting_anything) +
        nrow(splits_with_reversions[splits_with_reversions$reverts_all_previous_changes == "yes",])
    count_splits_never_reverted
    # - 76
    round(count_splits_never_reverted/nrow(splits) * 100, 2)
    # - 90.48%
    
    # Proportion of all splits
    round(nrow(splits_with_reversions_reverting_previous)/nrow(splits) * 100, 2)
    # - 36.9%
    
    # Interestingly, we can also figure this out by looking at splumps that revert all previous changes -- since technically those
    # that have no reversions also revert all previous!
    nrow(lumps[lumps$reverts_all_previous_changes == "yes",])
    # - 81 = 75 lumps not reverting anything + 10 lumps reverting a previous split
    # DISCREPENCY!!! Expected 81, got 81 (so is this no longer a problem?)
    
    nrow(splits[splits$reverts_all_previous_changes == "yes",])
    # - 76 = ???
    # DISCREPENCY!!! Expected 81, got 83 (+2)
    
    # We can do the same with perfect reversions.
    
    lumps_not_perfectly_reverting_anything <- lumps[lumps$perfect_reversion_count == 0,]
    nrow(lumps_not_perfectly_reverting_anything)
    # - 94 lumps
    
    lumps_with_perfect_reversions <- lumps[lumps$perfect_reversion_count > 0,]
    nrow(lumps_with_perfect_reversions)
    # - 21 lumps
    
    # Proportion of all lumps
    round(nrow(lumps_with_perfect_reversions)/nrow(lumps) * 100, 2)
    # - 18.26%
    
    # Of these 21 lumps, what proportion of them are reverting a previous split?
    lumps_with_perfect_reversions_reverting_previous <- lumps_with_perfect_reversions[lumps_with_perfect_reversions$perfectly_reverts_a_previous_change == "yes",]
    nrow(lumps_with_perfect_reversions_reverting_previous)
    # - 7 lumps
    
    # Proportion of all lumps
    round(nrow(lumps_with_perfect_reversions_reverting_previous)/nrow(lumps) * 100, 2)
    # - 6.09%
    
    count_lumps_never_perfectly_reverted <- 
        nrow(lumps_not_perfectly_reverting_anything) +
        nrow(lumps_with_perfect_reversions[lumps_with_perfect_reversions$reverts_all_previous_changes == "yes",])
    count_lumps_never_perfectly_reverted
    # - 99
    round(count_lumps_never_perfectly_reverted/nrow(lumps) * 100, 2)
    # - 86.09%
    
    splits_not_perfectly_reverting_anything <- splits[splits$perfect_reversion_count == 0,]
    nrow(splits_not_perfectly_reverting_anything)
    # - 64 splits
    
    splits_with_perfect_reversions <- splits[splits$perfect_reversion_count > 0,]
    nrow(splits_with_perfect_reversions)
    # - 20 splits
    
    # Of these 20 splits, what proportion of them are reverting a previous lump?
    splits_with_perfect_reversions_reverting_previous <- splits_with_perfect_reversions[splits_with_perfect_reversions$perfectly_reverts_a_previous_change == "yes",]
    nrow(splits_with_perfect_reversions_reverting_previous)
    # - 15 splits
    
    count_splits_never_perfectly_reverted <- 
        nrow(splits_not_perfectly_reverting_anything) +
        nrow(splits_with_perfect_reversions[splits_with_perfect_reversions$reverts_all_previous_changes == "yes",])
    count_splits_never_perfectly_reverted
    # - 78
    round(count_splits_never_perfectly_reverted/nrow(splits) * 100, 2)
    # - 92.86%
    
    # Interestingly, we can also figure this out by looking at splumps that revert all previous changes -- since technically those
    # that have no reversions also revert all previous!
    nrow(lumps[lumps$perfectly_reverts_all_previous_changes == "yes",])
    # - 99 = 
    # ??? 93 lumps not reverting anything + 7 lumps reverting a previous split
    # ??? DISCREPENCY!!! Expected 98, got 100
    
    nrow(splits[splits$perfectly_reverts_all_previous_changes == "yes",])
    # - 78 
    # ??? = 69 splits not reverting anything + 15 splits reverting a previous split
    # ??? DISCREPENCY!!! Expected 83, got 84
    
    # Okay, so what's the important summary here?
    # - Proportion of lumps that revert previous splits:
    nrow(splits_with_reversions_reverting_previous)/nrow(splits)
    (nrow(splits_with_reversions) - nrow(splits_with_reversions_reverting_previous))/nrow(splits)
    # ???
}
    
###############################################################
#### FISHER'S EXACT TEST OF LUMP -> SPLIT vs SPLIT -> LUMP ####
###############################################################

# What we want to compare:
#   - %age of lump reverted  

if(0) {
    lumps_with_perfect_reversions <- lumps[lumps$perfect_reversion_count >= 1,]
    lumps_with_perfect_reversions
    nrow(lumps_with_perfect_reversions)
    # - 27
    round(nrow(lumps_with_perfect_reversions)/nrow(lumps) * 100, 2)
    # - 19.01%
    
    perfect_lumps <- factor(lumps_with_perfect_reversions$perfect_reversions_summary)
    table(perfect_lumps)
    
    splits_with_perfect_reversions <- splits[splits$perfect_reversion_count >= 1,]
    splits_with_perfect_reversions
    nrow(splits_with_perfect_reversions)
    # - 27
    round(nrow(splits_with_perfect_reversions)/nrow(splits) * 100, 2)
    # - 28.42%
    
    perfect_splits <- factor(splits_with_perfect_reversions$perfect_reversions_summary)
    table(perfect_splits)
    
    # ALL TOGETHER NOW
    perfect_splumps <- splumps[splumps$perfect_reversion_count >= 1,]
    summary(perfect_splumps$type)
    write.csv(data.frame(table(perfect_splumps$perfect_reversions_summary)), "tables/perfect_reversions_summary.csv")
    
    # 
    df_perfect_reversions_summary <- data.frame(table(perfect_splumps$perfect_reversions_summary))
    df_perfect_reversions_summary
    
    # QUESTION: why do we only see part of the trajectory here, and not the complete trajectory?
    
    # Count 'em
    total_count <- length(perfect_splumps$perfect_reversions_summary)
    total_count
    # - 54
    count_lump_split_lump <- 2
    count_split_lump_split <- 2
    
    # Any others? You need to update the following calculations if you need to!
    
    count_lump_split <- sum(startsWith(as.character(perfect_splumps$perfect_reversions_summary), "lump")) - count_lump_split_lump
    count_lump_split
    # - 40
    count_split_lump <- sum(startsWith(as.character(perfect_splumps$perfect_reversions_summary), "split")) - count_split_lump_split
    count_split_lump
    # - 10
    count_total = count_lump_split_lump + count_split_lump_split + count_lump_split + count_split_lump
    count_total - total_count
    
    # BINOMIAL TEST: is there a 50-50 chance of lump_split vs split_lump?
    binom.test(c(count_lump_split, count_split_lump))
    # - 80%, p < 0.001
    
    # FISHER'S EXACT TEST TIME
    actual_lump_split <- count_lump_split
    actual_split_lump <- count_split_lump
    expected_lump_split <- nrow(splits)
    expected_split_lump <- nrow(lumps)
    
    fisher.test(
        matrix(data = c(actual_lump_split, expected_lump_split, actual_split_lump, expected_split_lump), nrow = 2, ncol = 2)
    )
    # p < 0.001, true odds ratio is not equal to 1: more splits after lump than expected if same proportion as lumps
}

lumps_with_perfect_reversions <- lumps[lumps$perfectly_reverts_a_previous_change == "yes",]
lumps_with_perfect_reversions
nrow(lumps_with_perfect_reversions)
# - 7
round(nrow(lumps_with_perfect_reversions)/nrow(lumps) * 100, 2)
# - 4.93

splits_with_perfect_reversions <- splits[splits$perfectly_reverts_a_previous_change == "yes",]
splits_with_perfect_reversions
nrow(splits_with_perfect_reversions)
# - 22
round(nrow(splits_with_perfect_reversions)/nrow(splits) * 100, 2)
# - 23.16%

# BINOMIAL TEST: is there a 50-50 chance of lump_split vs split_lump?
binom.test(c(nrow(lumps_with_perfect_reversions), nrow(splits_with_perfect_reversions)))
# - 24.14%, p < 0.01

lumps_with_partial_reversions <- lumps[lumps$reverts_a_previous_change == "yes",]
lumps_with_partial_reversions
nrow(lumps_with_partial_reversions)
# - 12
round(nrow(lumps_with_partial_reversions)/nrow(lumps) * 100, 2)
# - 8.45%

splits_with_partial_reversions <- splits[splits$reverts_a_previous_change == "yes",]
splits_with_partial_reversions
nrow(splits_with_partial_reversions)
# - 34
round(nrow(splits_with_partial_reversions)/nrow(splits) * 100, 2)
# - 35.79%

# BINOMIAL TEST: is there a 50-50 chance of lump_split vs split_lump?
binom.test(c(nrow(lumps_with_partial_reversions), nrow(splits_with_partial_reversions)))
# - 24.14%, p < 0.01

# FISHER'S EXACT TEST TIME
actual_lump_split <- nrow(splits_with_perfect_reversions)
actual_split_lump <- nrow(lumps_with_perfect_reversions)
expected_lump_split <- nrow(splits)
expected_split_lump <- nrow(lumps)

fisher.test(
    matrix(data = c(actual_lump_split, expected_lump_split, actual_split_lump, expected_split_lump), nrow = 2, ncol = 2)
)
# p < 0.001, true odds ratio is not equal to 1: more splits after lump than expected if same proportion as lumps

    
##################################
#### PART 6: SOURCE OF SPLITS ####
##################################

# Are most splits reverting previous lumps?

# Let's reload splumps just to make debugging easier.
splumps <- read.csv("../splumps/list.csv")

# Question N. How many lumps or splits have been reverted?
nrow(splumps)
# - 237
summary(splumps$type)
# - lump: 142
# - splits: 95

lumps <- splumps[splumps$type == "lump",]
splits <- splumps[splumps$type == "split",]

nrow(lumps)
# - 142
nrow(splits)
# - 95

# How many splits revert previous lumps?
sum(splits$perfect_reversion_count > 0)
# - 27
pc_splits_perfectly_reverting_lumps <- sum(splits$perfect_reversion_count > 0)/nrow(splits) * 100
round(pc_splits_perfectly_reverting_lumps, 2)
# 25.42%

splits_since_1980 <- splits[splits$year >= 1980,]
nrow(splits_since_1980)
# - 70 splits
splits_since_1980$year
# in range

sum(splits_since_1980$perfect_reversion_count > 0)
# - 15 splits
pc_splits_perfectly_reverting_lumps_since_1980 <- sum(splits_since_1980$perfect_reversion_count > 0)/nrow(splits_since_1980) * 100
round(pc_splits_perfectly_reverting_lumps_since_1980, 2)
# - 21.43%

splits_since_1950 <- splits[splits$year >= 1950,]
nrow(splits_since_1950)
# - 77 splits
splits_since_1950$year
# in range

sum(splits_since_1950$perfect_reversion_count > 0)
# - 19 splits
pc_splits_reverting_lumps_since_1950 <- sum(splits_since_1950$perfect_reversion_count > 0)/nrow(splits_since_1950) * 100
round(pc_splits_reverting_lumps_since_1950, 2)
# - 24.68%

#### Now with partial ####
sum(splits_since_1980$reversion_count > 0)
# - 25 splits
pc_splits_reverting_lumps_since_1980 <- sum(splits_since_1980$reversion_count > 0)/nrow(splits_since_1980) * 100
round(pc_splits_reverting_lumps_since_1980, 2)
# - 35.71%

####################################
#### PART 7. HIERARCHICAL MODEL ####
####################################

# - TODO: should the offset be scaled to the number of checklists, rather than the number of years?

# Reload, just 'cos.
taxon_concepts <- read.csv("../taxon_concepts/list.csv")
nrow(taxon_concepts)
# - 1226 circumscriptions

name_clusters_all <- read.csv("../currently_recognized/list.csv")
nrow(name_clusters_all)
# - 862 recognized names

# Duplicates?
sum(table(name_clusters_all$name) > 1)
# NO DUPLICATES!

# Now, let's get rid of the names added by aou_5_34
nrow(name_clusters_all)
# - 862
name_clusters <- name_clusters_all[name_clusters_all$taxon_concept_count > 0,]
nrow(name_clusters)
# 862

# Any extralimitals?
summary(name_clusters$order)
# YUP - 28 NAs! Eliminate.

name_clusters_no_extralimitals <- name_clusters[!is.na(name_clusters$order),]
nrow(name_clusters_no_extralimitals)
# 834

# How many eliminated?
862 - 834
# 28

# What's the mean definition count?
round(mean(name_clusters_no_extralimitals$taxon_concept_count), 3)
# - 1.391

sd(name_clusters_no_extralimitals$taxon_concept_count)
# 0.753

summary(name_clusters_no_extralimitals$taxon_concept_count > 1)
# false: 616, true: 218, NA: 0

# Histogram of the number of definitions per name.
start_export('hist_definition_counts')
par(cex=overall_cex*1.2)

# Sort by name
table_taxon_concept_counts <- table(name_clusters_no_extralimitals$taxon_concept_count)
table_taxon_concept_counts
table_taxon_concept_counts
barplot(table(name_clusters$taxon_concept_count),
    main = "Redescriptions amongst currently recognized species",
    ylab = "Frequency",
    xlab = "Number of taxon concepts",
    col = "blue"        
)
dev.off()
par(cex=overall_cex)

# We should see the same count as the latest AOU checklist
length(name_clusters_no_extralimitals$taxon_concept_count)
# - 834, yay

# Do we still see "hyemalis" as the toppermost of the poppermost?
name_clusters[which.max(name_clusters$taxon_concept_count),]$name
# - Yar.

# We shouldn't have any names with zero definitions.
sum(name_clusters_no_extralimitals$taxon_concept_count == 0)

# How many species have a single definition?
pc_single_defn <- sum(name_clusters_no_extralimitals$taxon_concept_count == 1)/nrow(name_clusters_no_extralimitals)
round(pc_single_defn * 100, 3)
# = 73.86%

# Okay, we need some higher taxonomy.
# And it's already in the file! Boom.
length(unique(factor(name_clusters_no_extralimitals$order)))
# - 25 orders
summary(is.na(name_clusters_no_extralimitals$order))
# - No NAs!

name_clusters_for_hierarchical_modeling <- name_clusters_no_extralimitals
nrow(name_clusters_for_hierarchical_modeling)
# - 834

# Re-factor the higher taxonomy
name_clusters_for_hierarchical_modeling$order <- factor(name_clusters_for_hierarchical_modeling$order)
# - 25 orders
name_clusters_for_hierarchical_modeling$family <- factor(name_clusters_for_hierarchical_modeling$family)
# - 80 families
name_clusters_for_hierarchical_modeling$genus <- factor(name_clusters_for_hierarchical_modeling$genus)
# - 338 genera

summary(is.na(name_clusters_for_hierarchical_modeling$order))
# No NAs!

length(unique(factor(name_clusters_for_hierarchical_modeling$family)))
# - 80 familes
summary(is.na(name_clusters_for_hierarchical_modeling$family))
# - contains NA: NO!

length(unique(factor(name_clusters_for_hierarchical_modeling$genus)))
# - 338 genera
summary(is.na(name_clusters_for_hierarchical_modeling$genus))
# - contains NA: NO!

# How long has this name been in the list?
# So that the minimum is >0, we use relative to 2017
summary(name_clusters_for_hierarchical_modeling$first_added_year)
summary(is.na(name_clusters_for_hierarchical_modeling$first_added_year))
# YAY!

hist(name_clusters_for_hierarchical_modeling$first_added_year)

name_clusters_for_hierarchical_modeling$years_in_list <- 2017 - name_clusters_for_hierarchical_modeling$first_added_year 
hist(name_clusters_for_hierarchical_modeling$years_in_list)

# But instead of years in list, we actually want the number of checklists *since* that year.
levels(name_clusters_for_hierarchical_modeling$first_added_dataset)
# - The following will ONLY work if these levels are in EXACTLY the right order!

name_clusters_for_hierarchical_modeling$available_in_checklists <- 
# How many checklists are we talking about here?
    length(levels(name_clusters_for_hierarchical_modeling$first_added_dataset))
# Subtract the index of the first checklist, so we go backwards: 0 means added in last checklist, 1 means in next to last, and so on.
    - as.integer(name_clusters_for_hierarchical_modeling$first_added_dataset) 
# So we don't have a zero
    + 1

# For STAN analyses, we'll be fitting a Poisson, so it makes more sense to
# think about the number of recircumscriptions, rather than the number
# of taxon concepts.
name_clusters_for_hierarchical_modeling$count_redescriptions <- name_clusters_for_hierarchical_modeling$taxon_concept_count - 1
name_clusters_for_hierarchical_modeling$count_redescriptions
mean(name_clusters_for_hierarchical_modeling$count_redescriptions)
# - 0.3908873

var(name_clusters_for_hierarchical_modeling$count_redescriptions)
# - 0.5673

# Get ready to STAN
Sys.setenv(PATH=paste("C:\\Rtools\\bin", "C:\\Rtools\\mingw_32\\bin", Sys.getenv("PATH"), sep=";"))
#cat('Sys.setenv(BINPREF = "C:/Program Files/mingw_$(WIN)/bin/")',
#    file = file.path(Sys.getenv("HOME"), ".Rprofile"), 
#    sep = "\n", append = TRUE)
Sys.getenv("PATH")
system("where make")
system("where g++")

devtools::find_rtools()

library(rstan)
rstan_options(auto_write = TRUE)
options(mc.cores = parallel::detectCores())

#### Prior predictive modelling ####
# This is a "true" prior predictive model, in which we generate name cluster values
# as Poisson values with a known lambda parameter, and see if we can retrieve it.

prior_predictive_lambda_0 <- mean(name_clusters_for_hierarchical_modeling$count_redescriptions)
prior_predictive_lambda_0
taxon_concept_count_for_prior <- rpois(nrow(name_clusters_for_hierarchical_modeling), prior_predictive_lambda_0)
taxon_concept_count_for_prior
# Looks about right.

mean(taxon_concept_count_for_prior)
# = 0.3741

# Prior predictive modelling
stan_d_prior <- list(
    nobs = nrow(name_clusters_for_hierarchical_modeling),
    norder = length(levels(name_clusters_for_hierarchical_modeling$order)),
    nfamily = length(levels(name_clusters_for_hierarchical_modeling$family)),
    ngenus = length(levels(name_clusters_for_hierarchical_modeling$genus)),
    y = taxon_concept_count_for_prior,
    order = as.integer(name_clusters_for_hierarchical_modeling$order),
    family = as.integer(name_clusters_for_hierarchical_modeling$family),
    genus = as.integer(name_clusters_for_hierarchical_modeling$genus),
    years_in_list = name_clusters_for_hierarchical_modeling$available_in_checklists
)

table(as.integer(name_clusters_for_hierarchical_modeling$family))
# Any missing? Nope!

model_fit_prior <- stan('counts_per_name_model.stan', data=stan_d_prior, 
    control = list(
        adapt_delta = 0.9
        # 1: There were 1 divergent transitions after warmup. Increasing adapt_delta above 0.8 may help. See
        # http://mc-stan.org/misc/warnings.html#divergent-transitions-after-warmup 
    ))
model_fit_prior
# - Rhats appear to be at 1.0!
post <- rstan::extract(model_fit_prior)
# - no Rhat above 1.1!

# Is this roughly the same as earlier?
hist(exp(post$log_lambda))
mean(exp(post$log_lambda))
# - 0.38097
prior_predictive_lambda_0
# - 0.3908
abline(v=prior_predictive_lambda_0, col="red")

#### Posterior predictive test ####
# Based on https://github.com/hmods/course/blob/master/slides/4-poisson/wk4_slides.pdf
post_lambda <- mean(exp(post$log_lambda))
post_lambda

# For each posterior draw: time until lump/split
n_time_until <- length(taxon_concept_count_for_prior)
n_iter <- nrow(post$log_lambda)
y_resp <- array(dim = c(n_iter, n_time_until))
var_resp <- rep(NA, n_iter)

for(i in 1:n_iter) {
    y_resp[i,] <- rpois(n_time_until, exp(post$log_lambda[i,]))
    var_resp[i] <- var(y_resp[i,])
}

mean(var_resp)
# - mean: 0.3809
hist(var_resp, breaks=40, xlab='Var(y)', main='Posterior predictive check for overdispersion')
# add red line for empirical variance
abline(v = var(taxon_concept_count_for_prior), col=2, lwd=3)
var(taxon_concept_count_for_prior)
# - 0.4024

#### Manipulated prior predictive modelling ####
# Now we take those lambda values from earlier and tweak some values to see if we
# can detect significant changes in particular higher taxa.
prior_predictive_lambda_0
# = 0.39088

# Now we manipulate that pure-poisson signal so we actually have
# some peaks, and see if we can recover them.
prior_multiplier <- 20

prior_predictive_lambda_0 <- mean(name_clusters_for_hierarchical_modeling$count_redescriptions)
prior_predictive_lambda_0
taxon_concept_count_for_prior <- as.data.frame(name_clusters_for_hierarchical_modeling)
taxon_concept_count_for_prior$taxon_concept_count <- rpois(nrow(name_clusters_for_hierarchical_modeling), prior_predictive_lambda_0)

index_branta_canadensis <- which(taxon_concept_count_for_prior$name == "Branta canadensis")
index_branta_canadensis
taxon_concept_count_for_prior[index_branta_canadensis,]$taxon_concept_count <-
    ceiling(prior_multiplier * (1 + taxon_concept_count_for_prior[index_branta_canadensis,]$taxon_concept_count))

index_branta_hutchinsii <- which(taxon_concept_count_for_prior$name == "Branta hutchinsii")
index_branta_hutchinsii
taxon_concept_count_for_prior[index_branta_hutchinsii,]$taxon_concept_count <-
    ceiling(prior_multiplier * (1 + taxon_concept_count_for_prior[index_branta_hutchinsii,]$taxon_concept_count))

index_grus_americana <- which(taxon_concept_count_for_prior$name == "Grus americana")
index_grus_americana
taxon_concept_count_for_prior[index_grus_americana,]$taxon_concept_count <- 
    ceiling(prior_multiplier * (1 + taxon_concept_count_for_prior[index_grus_americana,]$taxon_concept_count))

index_gavia_arctica <- which(taxon_concept_count_for_prior$name == "Gavia arctica")
index_gavia_arctica
taxon_concept_count_for_prior[index_gavia_arctica,]$taxon_concept_count <- 
    ceiling(prior_multiplier * (1 + taxon_concept_count_for_prior[index_gavia_arctica,]$taxon_concept_count))

index_gavia_pacifica <- which(taxon_concept_count_for_prior$name == "Gavia pacifica")
index_gavia_pacifica
taxon_concept_count_for_prior[index_gavia_pacifica,]$taxon_concept_count <- 
    ceiling(prior_multiplier * (1 + taxon_concept_count_for_prior[index_gavia_pacifica,]$taxon_concept_count))

index_chamaea_fasciata <- which(taxon_concept_count_for_prior$name == "Chamaea fasciata")
index_chamaea_fasciata
taxon_concept_count_for_prior[index_chamaea_fasciata,]$taxon_concept_count <- 
    ceiling(prior_multiplier * (1 + taxon_concept_count_for_prior[index_chamaea_fasciata,]$taxon_concept_count))

index_phoenicopterus_ruber <- which(taxon_concept_count_for_prior$name == "Phoenicopterus ruber")
index_phoenicopterus_ruber
taxon_concept_count_for_prior[index_phoenicopterus_ruber,]$taxon_concept_count <- 
    ceiling(prior_multiplier * (1 + taxon_concept_count_for_prior[index_phoenicopterus_ruber,]$taxon_concept_count))

# resulting counts:
taxon_concept_count_for_prior[index_branta_canadensis,]$taxon_concept_count
taxon_concept_count_for_prior[index_branta_hutchinsii,]$taxon_concept_count
taxon_concept_count_for_prior[index_grus_americana,]$taxon_concept_count
taxon_concept_count_for_prior[index_phoenicopterus_ruber,]$taxon_concept_count

# Prior predictive modelling
stan_d_prior <- list(
    nobs = nrow(name_clusters_for_hierarchical_modeling),
    norder = length(levels(name_clusters_for_hierarchical_modeling$order)),
    nfamily = length(levels(name_clusters_for_hierarchical_modeling$family)),
    ngenus = length(levels(name_clusters_for_hierarchical_modeling$genus)),
    y = taxon_concept_count_for_prior$taxon_concept_count, # no, you don't need to subtract 1 from this!
    order = as.integer(name_clusters_for_hierarchical_modeling$order),
    family = as.integer(name_clusters_for_hierarchical_modeling$family),
    genus = as.integer(name_clusters_for_hierarchical_modeling$genus),
    years_in_list = name_clusters_for_hierarchical_modeling$available_in_checklists
)

model_fit_prior <- stan('counts_per_name_model.stan', data=stan_d_prior, 
  control = list(
      adapt_delta = 0.99
      # 1: There were 1 divergent transitions after warmup. Increasing adapt_delta above 0.8 may help. See
      # http://mc-stan.org/misc/warnings.html#divergent-transitions-after-warmup 
  ))
model_fit_prior
# - Rhats appear to be at 1.0!
post <- rstan::extract(model_fit_prior)

#### Posterior predictive test ####
# Based on https://github.com/hmods/course/blob/master/slides/4-poisson/wk4_slides.pdf
mean(exp(post$log_lambda))
# 0.3986

# For each posterior draw: time until lump/split
n_time_until <- length(taxon_concept_count_for_prior$taxon_concept_count)
n_iter <- nrow(post$log_lambda)
y_resp <- array(dim = c(n_iter, n_time_until))
var_resp <- rep(NA, n_iter)

for(i in 1:n_iter) {
    y_resp[i,] <- rpois(n_time_until, exp(post$log_lambda[i,]))
    var_resp[i] <- var(y_resp[i,])
}

mean(var_resp)
# - 1.05
hist(var_resp, breaks=40, xlab='Var(y)', main='Posterior predictive check for overdispersion')
# add red line for empirical variance
abline(v = var(taxon_concept_count_for_prior$taxon_concept_count - 1), col=2, lwd=3)
var(taxon_concept_count_for_prior$taxon_concept_count - 1)
# - 0.7166

# Order level changes
library(dplyr)

# Order measurements.
order_mean <- apply(post$pi_i, 2, mean)
order_interval_min <- apply(post$pi_i, 2, function(x) { quantile(x, probs=0.025) } )
order_interval_max <- apply(post$pi_i, 2, function(x) { quantile(x, probs=0.975) } )
order_interval_width <- order_interval_max - order_interval_min
count_per_order <- taxon_concept_count_for_prior %>% group_by(order) %>% summarize(
    count = length(id),
    
    sum_redescriptions = sum(taxon_concept_count),
    mean_redescriptions = mean(taxon_concept_count)
)
order_measurements <- data.frame(
    row.names=levels(count_per_order$order),
    sum_redescriptions = count_per_order$sum_redescriptions,
    mean_redescriptions = count_per_order$mean_redescriptions,
    count=count_per_order$count,
    min=order_interval_min,
    mean=order_mean,
    max=order_interval_max,
    significant=ifelse(((order_interval_min > 0 & order_interval_max > 0) == 1) | ((order_interval_min < 0 & order_interval_max < 0) == 1), "yes", "no"),
    interval_width=order_interval_width
)
order_measurements
order_measurements[order_measurements$significant == "yes",]

# Plot
# count number of observations per order
#plot(order_interval_width ~ count_per_order$count, ylab="Interval width", xlab="Number of observations per order", main="5% credible interval widths for number of observations")

# FAMILY
family_mean <- apply(post$tau_j, 2, mean)
family_interval_min <- apply(post$tau_j, 2, function(x) { quantile(x, probs=0.025) } )
family_interval_max <- apply(post$tau_j, 2, function(x) { quantile(x, probs=0.975) } )
family_interval_width <- family_interval_max - family_interval_min
count_per_family <- taxon_concept_count_for_prior %>% group_by(family) %>% summarize(
    count = length(id),

    sum_redescriptions = sum(taxon_concept_count),
    mean_redescriptions = mean(taxon_concept_count)    
)
family_measurements <- data.frame(
    row.names=levels(count_per_family$family),
    sum_redescriptions = count_per_family$sum_redescriptions,
    mean_redescriptions = count_per_family$mean_redescriptions,
    count=count_per_family$count,
    min=family_interval_min,
    mean=family_mean,
    max=family_interval_max,
    significant=ifelse(((family_interval_min > 0 & family_interval_max > 0) == 1) | ((family_interval_min < 0 & family_interval_max < 0) == 1), "yes", "no"),
    interval_width=family_interval_width
)
family_measurements
family_measurements[family_measurements$significant == "yes",]

# Plot
# plot(family_interval_width ~ count_per_family$count, ylab="Interval width", xlab="Number of observations per family", main="5% credible interval widths for number of observations")

# GENUS
genus_mean <- apply(post$rho_k, 2, mean)
genus_interval_min <- apply(post$rho_k, 2, function(x) { quantile(x, probs=0.025) } )
genus_interval_max <- apply(post$rho_k, 2, function(x) { quantile(x, probs=0.975) } )
genus_interval_width <- genus_interval_max - genus_interval_min
count_per_genus <- taxon_concept_count_for_prior %>% group_by(genus) %>% summarize(
    count = length(id), 
    sum_redescriptions = sum(taxon_concept_count),
    mean_redescriptions = mean(taxon_concept_count)
)
genus_measurements <- data.frame(
    row.names=levels(count_per_genus$genus),
    sum_redescriptions=count_per_genus$sum_redescriptions,
    mean_redescriptions=count_per_genus$mean_redescriptions,    
    count=count_per_genus$count,
    min=genus_interval_min,
    mean=genus_mean,
    max=genus_interval_max,
    significant=ifelse(((genus_interval_min > 0 & genus_interval_max > 0) == 1) | ((genus_interval_min < 0 & genus_interval_max < 0) == 1), "yes", "no"),
    interval_width=genus_interval_width
)
genus_measurements
genus_measurements[genus_measurements$significant == "yes",]
# View(genus_measurements)

prior_multiplier

# CONCLUSION OF PRIOR PREDICTIVE TESTING (TEST 6)
# - at a prior_multiplier of 2.5 -> 0 genera
# - at a prior_multiplier of 7 -> 0 genera, which is odd, because we're overdispersed like crazy now. um, well.
# - at a prior_multiplier of 1.5 -> 90 genera
# - at a prior_multiplier of 10 -> 5 genera (exactly the genera that I expected!)
# - at a prior_multiplier of 20 -> 

# CONCLUSION OF PRIOR PREDICTIVE TESTING (TEST 5)
# - at a prior_multiplier of 2.5 -> 26 genera (presumably because 'test 5' starts with the actual data)

# CONCLUSION OF PRIOR PREDICTIVE TESTING (TEST 4)
# - at a prior_multiplier of 20 -> two orders, three genera
# - at a prior_multiplier of 11 -> four genera
# - at a prior_multiplier of 7 -> five genera
# 
# CONCLUSION OF PRIOR PREDICTIVE TESTING (TEST 3)
# - at a prior_multiplier of 20 -> three genera, no families, two orders
# - at a prior_multiplier of 15 -> many genera, no families, no orders
# - at a prior_multiplier of 12 -> many genera, no families, no orders
# - at a prior_multiplier of 11 -> 
# - at a prior_multiplier of 10 -> several genera, no families, no orders
# - at a prior_multiplier of 9 -> three genera, no families, no orders
# - at a prior_multiplier of 8 -> two genera, two orders, no families
# - at a prior_multiplier of 7 -> genus and families significant, but not order
#   -> retry: genus, no families, one order
# - at a prior_multiplier of 5 -> nothing significant
# - at a prior_multiplier of 3 -> nothing significant

# CONCLUSION OF PRIOR PREDICTIVE TESTING (TEST 2)
# - at a prior_multiplier of 200 -> 
# - at a prior_multiplier of 100 -> one family significant, two orders significant
# - at a prior_multiplier of 50 -> four families, no orders
# - at a prior_multiplier of 20 -> families become significant, orders no longer significant
# - at a prior_multiplier of 18 -> 
# - at a prior_multiplier of 15 -> 
# - at a prior_multiplier of 12 -> multiple orders are significant
# - at a prior_multiplier of 11 -> one order, one family 
# - at a prior_multiplier of 10 -> 
# - at a prior_multiplier of 9 -> only one order is significant
# - at a prior_multiplier of 5, 7 -> orders are significant.
# - at a prior_multiplier of 4.8 -> 
# - at a prior_multiplier of 4.5 -> nothing is significant.
# - at a prior_multiplier of 4.2 -> 
# - at a prior_multiplier of 3, 4 -> nothing is significant.
# - at a prior_multiplier of 2.8 -> nothing is significant
# - at a prior_multiplier of 2.5 -> 
# - at a prior_multiplier of 2.2 -> nothing is significant
# - at a prior_multiplier of 1.8 -> nothing is significant
# - at a prior_multiplier of 1.5 -> four families, no genera or families
# - at a prior_multiplier of 1.2 -> nothing is significant

#### Final model ####
mean(name_clusters_for_hierarchical_modeling$count_redescriptions)
# - 0.3908873

# STAN model.
stan_d <- list(
    nobs = nrow(name_clusters_for_hierarchical_modeling),
    norder = length(levels(name_clusters_for_hierarchical_modeling$order)),
    nfamily = length(levels(name_clusters_for_hierarchical_modeling$family)),
    ngenus = length(levels(name_clusters_for_hierarchical_modeling$genus)),
    y = name_clusters_for_hierarchical_modeling$count_redescriptions,
    order = as.integer(name_clusters_for_hierarchical_modeling$order),
    family = as.integer(name_clusters_for_hierarchical_modeling$family),
    genus = as.integer(name_clusters_for_hierarchical_modeling$genus),
    years_in_list = name_clusters_for_hierarchical_modeling$available_in_checklists
)

model_fit <- stan('counts_per_name_model.stan', data=stan_d, 
    control = list(
        adapt_delta = 0.99
        # 1: There were 1 divergent transitions after warmup. Increasing adapt_delta above 0.8 may help. See
        # http://mc-stan.org/misc/warnings.html#divergent-transitions-after-warmup 
    )
, iter=5000)
model_fit
# - Rhats appear to be at 1.0!
post <- rstan::extract(model_fit)

library(shinystan)
shinystan::launch_shinystan(model_fit)

# Examine the results of the plot.
#traceplot(model_fit, inc_warmup=T)

hist(exp(post$log_lambda))
hist((post$lambda_0))
hist((post$sigma_i))
hist((post$sigma_j))
hist((post$sigma_k))

# Overall rate
mean(exp(post$log_lambda))
# - 0.39868

# For each posterior draw: time until lump/split
n_time_until <- length(name_clusters_for_hierarchical_modeling$count_redescriptions)
n_iter <- nrow(post$log_lambda)
y_resp <- array(dim = c(n_iter, n_time_until))
var_resp <- rep(NA, n_iter)

for(i in 1:n_iter) {
    y_resp[i,] <- rpois(n_time_until, exp(post$log_lambda[i,]))
    var_resp[i] <- var(y_resp[i,])
}

mean(y_resp)
# - 0.3985
mean(var_resp)
# - 0.6668

hist(var_resp, breaks=40, xlab='Var(y)', main='Posterior predictive check for overdispersion')
# add red line for empirical variance
abline(v = var(name_clusters_for_hierarchical_modeling$count_redescriptions), col=2, lwd=3)
var(name_clusters_for_hierarchical_modeling$count_redescriptions)
# - 0.5673

# Order level changes
library(dplyr)

# We need to save this into files.
filename_postfix <- '_pre1982'

# Order measurements.
order_mean <- apply(post$pi_i, 2, mean)
order_interval_min <- apply(post$pi_i, 2, function(x) { quantile(x, probs=0.025) } )
order_interval_max <- apply(post$pi_i, 2, function(x) { quantile(x, probs=0.975) } )
order_interval_width <- order_interval_max - order_interval_min
count_per_order <- name_clusters_for_hierarchical_modeling %>% group_by(order) %>% summarize(
    count = length(id),
    
    sum_redescriptions = sum(count_redescriptions),
    mean_redescriptions = mean(count_redescriptions)
)
order_measurements <- data.frame(
    row.names=levels(count_per_order$order),
    sum_redescriptions = count_per_order$sum_redescriptions,
    mean_redescriptions = count_per_order$mean_redescriptions,
    count=count_per_order$count,
    min=order_interval_min,
    mean=order_mean,
    max=order_interval_max,
    significant=ifelse(((order_interval_min > 0 & order_interval_max > 0) == 1) | ((order_interval_min < 0 & order_interval_max < 0) == 1), "yes", "no"),
    interval_width=order_interval_width
)
order_measurements
order_measurements[order_measurements$significant == "yes",]
write.csv(order_measurements, file=paste(sep="", 'tables/table_s4_hmod_order', filename_postfix, '.csv'))

# Plot
# count number of observations per order
plot(order_interval_width ~ count_per_order$count, ylab="Interval width", xlab="Number of observations per order", main="5% credible interval widths for number of observations")

# FAMILY
family_mean <- apply(post$tau_j, 2, mean)
family_interval_min <- apply(post$tau_j, 2, function(x) { quantile(x, probs=0.025) } )
family_interval_max <- apply(post$tau_j, 2, function(x) { quantile(x, probs=0.975) } )
family_interval_width <- family_interval_max - family_interval_min
count_per_family <- name_clusters_for_hierarchical_modeling %>% group_by(family) %>% summarize(
    count = length(id),
    
    sum_redescriptions = sum(count_redescriptions),
    mean_redescriptions = mean(count_redescriptions)
)
family_measurements <- data.frame(
    row.names=levels(count_per_family$family),
    sum_redescriptions = count_per_family$sum_redescriptions,
    mean_redescriptions = count_per_family$mean_redescriptions,
    count=count_per_family$count,
    min=family_interval_min,
    mean=family_mean,
    max=family_interval_max,
    significant=ifelse(((family_interval_min > 0 & family_interval_max > 0) == 1) | ((family_interval_min < 0 & family_interval_max < 0) == 1), "yes", "no"),
    interval_width=family_interval_width
)
family_measurements
family_measurements[family_measurements$significant == "yes",]
write.csv(family_measurements, file=paste(sep="", 'tables/table_s5_hmod_family', filename_postfix, '.csv'))

# Plot
plot(family_interval_width ~ count_per_family$count, ylab="Interval width", xlab="Number of observations per family", main="5% credible interval widths for number of observations")

# GENUS
genus_mean <- apply(post$rho_k, 2, mean)
genus_interval_min <- apply(post$rho_k, 2, function(x) { quantile(x, probs=0.025) } )
genus_interval_max <- apply(post$rho_k, 2, function(x) { quantile(x, probs=0.975) } )
genus_interval_width <- genus_interval_max - genus_interval_min
count_per_genus <- name_clusters_for_hierarchical_modeling %>% group_by(genus) %>% summarize(
    count = length(id),
    
    sum_redescriptions = sum(count_redescriptions),
    mean_redescriptions = mean(count_redescriptions)
)
genus_measurements <- data.frame(
    row.names=levels(count_per_genus$genus),
    sum_redescriptions = count_per_genus$sum_redescriptions,
    mean_redescriptions = count_per_genus$mean_redescriptions,
    count=count_per_genus$count,
    min=genus_interval_min,
    mean=genus_mean,
    max=genus_interval_max,
    significant=ifelse(((genus_interval_min > 0 & genus_interval_max > 0) == 1) | ((genus_interval_min < 0 & genus_interval_max < 0) == 1), "yes", "no"),
    interval_width=genus_interval_width
)
genus_measurements
write.csv(genus_measurements, file=paste(sep="", 'tables/table_s6_hmod_genus', filename_postfix, '.csv'))

genus_measurements[genus_measurements$significant == "yes",]
# 
nrow(genus_measurements[genus_measurements$significant == "yes",])
# - 24 genera
nrow(genus_measurements[genus_measurements$significant == "yes",])/nrow(genus_measurements)
# - 7.1% of
nrow(genus_measurements)
# - 338

# What's the higher taxonomy of these genera?
genera <- row.names(genus_measurements[genus_measurements$significant == "yes",])
length(genera)
index <- 0
order <- c()
family <- c()
for(genus in genera) {
    index <- index + 1
    orders <- unique(name_clusters_for_hierarchical_modeling[name_clusters_for_hierarchical_modeling$genus == genus,]$order)
    families <- unique(name_clusters_for_hierarchical_modeling[name_clusters_for_hierarchical_modeling$genus == genus,]$family)
    # print(paste(index, ".", genus, "is in family", families, "and in orders", orders))
    order <- append(order, as.character(orders))
    family <- append(family, as.character(families))
}
order
table(order)
length(table(order))
# - 8 orders
length(table(order))/length(unique(name_clusters_for_hierarchical_modeling$order))
# - 32% of all orders

family
table(family)
length(table(family))
# - 16 families
length(unique(name_clusters_for_hierarchical_modeling$family))
length(table(family))/length(unique(name_clusters_for_hierarchical_modeling$family))
# - 20% of families

# Plot
plot(genus_interval_width ~ count_per_genus$count, ylab="Interval width", xlab="Number of observations per genus", main="5% credible interval widths for number of observations")

length(genera)
# - 23 genera

genus_measurement_counts <- genus_measurements$count
summary(genus_measurement_counts)
#    Min. 1st Qu.  Median    Mean 3rd Qu.    Max. 
#   1.000   1.000   1.500   2.467   3.000  26.000 
significant_genera_counts <- genus_measurements[genus_measurements$significant == "yes",]$count
summary(significant_genera_counts)
#    Min. 1st Qu.  Median    Mean 3rd Qu.    Max. 
#   2.00    2.00    3.00    3.87    5.00   11.00

sort(genus_measurement_counts)

hist(genus_measurement_counts)
table(genus_measurement_counts)
hist(significant_genera_counts, col="red", add=T)
table(significant_genera_counts)

# Genera with synonyms!
name_clusters[name_clusters$genus %in% genera,]$all_names_in_cluster

# are these genera special in terms of their numbers of years in this checklist?
nrow(name_clusters)
hist(name_clusters$first_added_year)
table(name_clusters$first_added_year)
652/sum(table(name_clusters$first_added_year))
hist(name_clusters[name_clusters$genus %in% genera,]$first_added_year, add=T, col="red")
table(name_clusters[name_clusters$genus %in% genera,]$first_added_year)
55/sum(table(name_clusters[name_clusters$genus %in% genera,]$first_added_year))

data.frame(genera)
# Mayr and Short 1970:
#
# Hybridization (pg 93):
#   1      Ammodramus
#   2           Anser - A. Intraspecific hybridization: gamma polymorphism
#   3      Aphelocoma
#   4  Artemisiospiza 
#   5      Baeolophus
#   6          Branta - A. Intraspecific hybridization: beta extent of hybridization limited or uncertain
#   7       Butorides
#   8     Calonectris
#   9     Dendragapus - A. Intraspecific hybridization: alpha hybrid zone
#   10      Empidonax
#   11      Gallinago
#   12      Gallinula
#   13          Junco - A. Intraspecific hybridization: alpha hybrid zone, Group C. Limited hybridization between sympatric species not belonging to the same superspecies
#   14    Leucosticte - A. Intraspecific hybridization: alpha hybrid zone
#   15    Limnodromus 
#   16      Melanitta  
#   17       Melozone
#   18       Puffinus
#   19      Quiscalus
#   20         Rallus - B. Interspecific hybridization between largely allopatric members of the same superspecies: beta limited hybridization in contact or overlap zone
#   21       Sternula 
#   22           Sula 
#   23    Troglodytes - A. Intraspecific hybridization
#
# Group A. Intraspecific hybridization: alpha hybrid zone
#   - Missing: Anas, Colaptes, Perisoreus, Parus, Psaltriparus, Dendroica, Quiscalus,
#       Icterus, Pipilo, Aimophila, 
# Group A. Intraspecific hybridization: beta hybrid zone
#   - Missing: Anas, Otus, Corvus, Oporornis, Carduelis
# Group A. Intraspecific hybridization: gamma polymorphism
#   - Missing: Ardea, Buteo, Psaltriparus, Charadrius
# Group B. Interspecific hybridization between largely allopatric members of the same superspecies: alpha zones of overlap and extensive hybridization
#   - Missing: Anas, Larus, Vermivora, Pheucticus, Passerina
# Group B. Interspecific hybridization between largely allopatric members of the same superspecies: beta limited hybridization in contact or overlap zone
#   - Missing: Gavia, Callipepla, Charadrius, Picoides, Sphyrapicus, Contopus, Parus, Dendroica, Sturnella, Piranga, Acanthis
# Group C. Limited hybridization between sympatric species not belonging to the same superspecies
#   - Missing: Tympanuchus, Callipepla, Archilochus, Archilochus, Picoides, Zonotrichia
# 
# (main table)   
#   All values for North American range only
#   Groups:
#       - A: Waterbirds
#       - B: hawks, gallinceous birds
#       - C: rails, cranes, shorebirds, gulls
#       - D: pigeons, parrots, cuckoos
#       - E: owls, hummingbirds, woodpeckers
#       - F: tyrant flycatchers
#       - G: larks, swallows, crows, Old World oscines
#       - H: nine-primaried oscines
#
#   status:
#       - A: monotypic species
#       - B: uncomplicated polytypic species
#       - C: strongly differentiated polytypic species
#       - D: member of superspecies
#       - E: member of species group
#   
#   1      Ammodramus (group H) - A, B, C
#   2           Anser (group A) - E
#   3      Aphelocoma (group G) - C
#   4  Artemisiospiza 
#   5      Baeolophus
#   6          Branta (group A) - C, D, E 
#   7       Butorides
#   8     Calonectris
#   9     Dendragapus
#   10      Empidonax (group F) - A, B, C, D, E
#   11      Gallinago (group C) - A
#   12      Gallinula (group C) - A
#   13          Junco (group H) - A, C, D
#   14    Leucosticte -- prob not in Mayr and Short
#   15    Limnodromus 
#   16      Melanitta (group ) - 
#   17       Melozone
#   18       Puffinus
#   19      Quiscalus
#   20         Rallus (group C) - D
#   21       Sternula [=Sterna] (group C) - A, B, E (+ C, D in global range)
#   22           Sula (group Maine Species) - A
#   23    Troglodytes (group G) - B, C, D, E