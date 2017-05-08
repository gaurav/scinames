#
# ANALYSIS FOR THE AOU PAPER BY YEAR (May 2, 2017)
#
# This analysis file includes instructions for both full analyses as well
# as pre-1982 analyses. Set the FLAG_PRE1982 = T if you would like to
# perform this analysis; otherwise, set FLAG_PRE1982 = F
#

getwd()

# Set up Windows fonts
windowsFonts(Calibri=windowsFont("Calibri"))
par(family="Calibri")

# - TODO: Note that we do have multiple checklists within the same year 
# sometimes, so make sure all our per-year measurements account for that.
# - TODO: pre-1982 or pre-1983? We've standardized to pre-1982 for now, but
# maybe the other one makes more sense?

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
# lump: 128
# splits: 189

all_name_clusters <- read.csv("../currently_recognized/list-2127.csv")
nrow(all_name_clusters)
# - 2127 name clusters
sum(all_name_clusters$taxon_concept_count)
#  - 2,597 taxon concepts

# - 3087 taxon concepts without pre1982 filtering

project_stats_all <- read.csv("../project_stats/list-all.csv")
nrow(project_stats_all)
# 66 checklists, including duplicates of aou_5_34 and others
data.frame(project_stats_all$dataset, project_stats_all$binomial_count)
# - aou_5_33: 860
# - aou_5_34: 940
# - aou_6: 1911

#####################################
#### OTHER INTRODUCTORY MATERIAL ####
#####################################

splumps <- read.csv("../splumps/list.csv")
summary(splumps$type)
# lump: 122
# splits: 93

project_stats <- read.csv("../project_stats/list.csv")
nrow(project_stats)
data.frame(project_stats$dataset, project_stats$binomial_count)

summary(project_stats$binomial_count)
binomial_count_by_year <- tapply(
    project_stats$binomial_count,
    project_stats$year,
    max
)
sort(binomial_count_by_year)

##############################
#### PART 2: LUMPS/SPLITS ####
##############################

# Load splumps
splumps <- read.csv("../splumps/list.csv")

# How many splumps in total?
summary(splumps$type)
# lump: 122
# split: 93

lumps <- splumps[splumps$type == "lump",]
nrow(lumps)
# - 122 lumps

splits <- splumps[splumps$type == "split",]
nrow(splits)
# - 93 splits

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
# - 1173 added
sum(supplement_counts$count_deleted)
# - 372 deleted
sum(supplement_counts$count_lump)
# - 122 lumps
sum(supplement_counts$count_split)
# - 93 splits

# How many checklists do we have?
data.frame(supplement_counts$dataset, supplement_counts$year)
years <- unique(sort(supplement_counts$year))
length(years)
# 64 checklists = 57 supplements + 7 major editions - 1 (because aou_1_07 and aou_2 both take place in 1895) + 1 NACC_latest

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
# - 215
length(zoo_lumps)
# - 64
sum(zoo_lumps)
# - 122
length(zoo_splits)
# - 64
sum(zoo_splits)
# - 93

# Can we compensate for the number of recognized species in each case?
summary(supplement_counts$binomial_count)
binomial_count_by_year <- tapply(
    supplement_counts$binomial_count,
    supplement_counts$year,
    max
)
sort(binomial_count_by_year)
# min: 771 in 1886
# max: 1045 in 2016
zoo_binomial_count_by_year <- zoo(binomial_count_by_year, names(binomial_count_by_year))

# Checklists with zero splumps
splumps_by_year_with_zero <- splumps_by_year[splumps_by_year == 0]
length(splumps_by_year_with_zero)
# 12 checklists, including 1886 and 2017, which shouldn't count
splumps_by_year_with_zero

#### FIGURE 1a. Bar graph of lumps and splits with cumulative curves ####

library(Hmisc)
start_export('cumul_lumps_and_splits_bargraph', width=1000, height=600)
max_ylim <- max(sum(zoo_lumps), sum(zoo_splits))
par(mfrow=c(1, 1), cex=overall_cex*0.7)
plot(cumsum(zoo_lumps),
     ylim=c(0, max_ylim), 
     col=2, 
     ylab="Number of changes", 
     xlab="Year"
     # main="Cumulative lumps and splits from 1889 to 2016"
)
minor.tick(nx=10, ny=0)
par(new=T)
#plot(cumsum(zoo_lumps), ylim=c(0, sum(zoo_lumps)), col=2, ylab=NA, xlab=NA, axes=F)
#points(zoo_lumps, col=2, pch=4)
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
par(mfrow=c(1, 1), cex=overall_cex)

#### FIGURE NA: Gap size correlated with post-gap measurement ####

# Do measurements correlate with a lagged measure?
lagged <- c(0, lag(zoo_splumps, k=-1))
lagged
plot(log(zoo_splumps), log(lagged))
cor.test(zoo_splumps, lagged)
# Yes, yes they do (r = 0.346, p < 0.01)
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
# - adjR2 = 0.64
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
# - adjR2 = 0.67
abline(gap_model_lump, lty=2)

# Plot 3. Splumps
plot(gap_analysis$splits ~ gap_analysis$gap,
     main = "Splits only",
     xlab = "Gap (years)",
     ylab = "Number of changes"
)
gap_model_splits <- lm(gap_analysis$splits ~ gap_analysis$gap)
summary(gap_model_splits)
# - p = 0.167 > 0.05
# - multR2 = 0.031
# - adjR2 = 0.015
abline(gap_model_splits, lty=2)

# Done! Reset par.
dev.off()
par(mfrow=c(1, 1), cex=overall_cex)

######################################
#### PART 4: Count latest species ####
######################################

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

# Described since 1950
round(14/2127 * 100, 2)
# - 0.66%

round(101/2127 * 100, 2)
# - 101 since 1900 (4.75%)

round(169/2127 * 100, 2)
# - 169 since 1889 (7.95%) 

round(198/2127 * 100, 2)
# - 198 since 1885 (9.31%) 

# What are the proportions of the species in this study?
name_clusters <- read.csv("../currently_recognized/list.csv")
nrow(name_clusters)
# - 1045 name clusters

sum(name_clusters$taxon_concept_count)
# - 1205 taxon concepts

name_clusters$species_lc <- tolower(name_clusters$species)
name_clusters$name_lc <- tolower(name_clusters$name)

# This is broken; don't use without fixing!
if(0) {
    name_clusters_with_desc <- merge(name_clusters, original_descs, by.x = "name_lc", by.y = "species_lc", all.x = TRUE)
    nrow(name_clusters_with_desc)
    # - still 974
    
    # Any NAs?
    nrow(name_clusters_with_desc)
    summary(name_clusters_with_desc$year)
    # - 919 
    
    # For-now answer
    summary(name_clusters_with_desc$year)
    name_clusters_years_without_na <- name_clusters_with_desc[which(!is.na(name_clusters_with_desc$year)),]
    nrow(name_clusters_years_without_na)
    # - 948
    
    cumsum(table(name_clusters_years_without_na$year))
    cumsum(rev(table(name_clusters_years_without_na$year)))
    
    # Proportion before 1900
    round(21/948 * 100, 2)
    # - 2.22%
}

#####################################
#### PART 5: Additions/deletions ####
#####################################

# Note: if we ever actually use these numbers anywhere, 
# remember that this INCLUDES NACC_latest -- so the 
# additions and deletions are inflated here!

# Note: it also includes aou_1.txt, so that's an awful
# lot of additions right there huh.

supplement_counts <- read.csv("../project_stats/list.csv")

sum(supplement_counts$count_added)
# - 1173 added
sum(supplement_counts$count_deleted)
# - 372 deleted
sum(supplement_counts$count_lump)
# - 122 lumps
sum(supplement_counts$count_split)
# - 93 splits

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

#### Where do lumping and splitting spike?
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
# - 93 splits
splits_by_year[names(splits_by_year) >= 1982]
sum(splits_by_year[names(splits_by_year) >= 1982])
# - 70 splits
pc_splits_after_1982 <- sum(splits_by_year[names(splits_by_year) >= 1982])/sum(splits_by_year) * 100
round(pc_splits_after_1982, 2)
# - 75.27%
70/93

sum(splits_by_year[names(splits_by_year) >= 1980])
# - 70 splits
round(100 - pc_splits_after_1982, 2)
# - missing: 23.91%

################################
#### PART 6: TAXON CONCEPTS ####
################################

# We don't do any filtering for currently-recognized, so that's the only
# taxon concepts we're interested in. So make sure these come from the
# data reconciliation service!
taxon_concepts <- read.csv("../taxon_concepts/list.csv")
nrow(taxon_concepts)
# - 1205 circumscriptions

length(table(taxon_concepts$name_cluster_id))
# - 862 from name clusters

name_clusters_all <- read.csv("../currently_recognized/list.csv")
nrow(name_clusters_all)
table(name_clusters_all$id)
# - 1045 from name clusters

# ANY DUPLICATES?
which(table(name_clusters_all$id) > 1)
# - 0 -- hooray!
# name_clusters[name_clusters$id == "71bcdf15-7b5e-4d52-9689-d7dbcab29147",]

# Okay, thanks to the wonder of SciNames, we already have the counts ...
summary(name_clusters_all$taxon_concept_count)
#   0.000   1.000   1.000   1.153   1.000   5.000

# Wait, zeros?
nrow(name_clusters_all[name_clusters_all$taxon_concept_count == 0,])
# - 183 zeros

name_clusters_all[name_clusters_all$taxon_concept_count == 0,]$name
# - It looks like these are all names that have been eliminated by the filter,
#   but are still recognized because they were introduced (mainly in aou_5_34)

# So let's eliminate them!
name_clusters <- name_clusters_all[name_clusters_all$taxon_concept_count > 0,]

# How many name clusters do we have now? Lemme guess ...
nrow(name_clusters)
# - 862! woo!

sum(is.na(name_clusters$taxon_concept_count))
# - 0 NAs
sum(name_clusters$taxon_concept_count)
# - 1205 taxon concepts
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
# - 1205 taxon concepts

# What's the max?
max(taxon_concepts_per_name)

# - Get rid of "(not found in dataset)", which is the highest.
# taxon_concepts_per_name[which.max(taxon_concepts_per_name)] <- NA
taxon_concepts_per_name[which.max(taxon_concepts_per_name)]
# - 6 ()
taxon_concepts[taxon_concepts$name_cluster_id == "f1ce9f45-cedb-4f10-a85f-09c348421ab6",]
#    - And yes, it's Junco hyemalis!

summary(taxon_concepts_per_name)
#   Min. 1st Qu.  Median    Mean 3rd Qu.    Max. 
#   1.000   1.000   1.000   1.398   2.000   6.000
summary(name_clusters$taxon_concept_count)
#   1.000   1.000   1.000   1.398   2.000   6.000

name_clusters$taxon_concept_count
sum(name_clusters$taxon_concept_count)
# - 1205 taxon concepts

# How many currently recognized species have been corrected more than once?
# (We call this the "correction rate" in the paper.)

total_recognized_name_clusters = nrow(name_clusters)
total_recognized_name_clusters
# - 862 name clusters

count_name_clusters_exactly_one <- sum(name_clusters$taxon_concept_count == 1)
count_name_clusters_exactly_one
# - 631
stability <- count_name_clusters_exactly_one / total_recognized_name_clusters
round(stability * 100, 2)

count_name_clusters_more_than_one <- sum(name_clusters$taxon_concept_count > 1)
count_name_clusters_more_than_one
# - 231 name clusters
correction_rate <- count_name_clusters_more_than_one / total_recognized_name_clusters
round(correction_rate * 100, 2)
# - 26.8

count_name_clusters_exactly_two <- sum(name_clusters$taxon_concept_count == 2)
count_name_clusters_exactly_two
# - 140
single_correction <- count_name_clusters_exactly_two / total_recognized_name_clusters
round(single_correction * 100, 2)
# - 16.24%

count_name_clusters_two_or_more <- sum(name_clusters$taxon_concept_count >= 2)
count_name_clusters_two_or_more
# - 231 name clusters
recorrection_rate <- count_name_clusters_two_or_more / total_recognized_name_clusters
round(recorrection_rate * 100, 2)
# - 26.8%

count_name_clusters_more_than_two <- sum(name_clusters$taxon_concept_count > 2)
count_name_clusters_more_than_two
# - 91 name clusters
recorrection_rate <- count_name_clusters_two_or_more / total_recognized_name_clusters
round(recorrection_rate * 100, 2)
# - 10.56%

# Let's reload splumps just to make debugging easier.
splumps <- read.csv("../splumps/list.csv")

# Question N. How many lumps or splits have been reverted?
nrow(splumps)
# - 215
summary(splumps$type)
# - lump: 122
# - splits: 93

lumps <- splumps[splumps$type == "lump",]
splits <- splumps[splumps$type == "split",]

nrow(lumps)
# - 122
nrow(splits)
# - 93

# Perfect reversions, man, I don't know.
(splumps$perfect_reversion_count >= 1)

sum(lumps$reversion_count >= 1)
# - 44 lumps
reversion_pc_lumps = sum(lumps$reversion_count >= 1)/nrow(lumps)
round(reversion_pc_lumps * 100, 2)
# - lumps: 36.07%

sum(splits$reversion_count >= 1)
# - 35 splits
reversion_pc_splits = sum(splits$reversion_count >= 1)/nrow(splits)
round(reversion_pc_splits * 100, 2)
# - splits: 44.09%

sum(lumps$perfect_reversion_count >= 1)
# - 24 lumps
perfect_reversion_pc_lumps = sum(lumps$perfect_reversion_count >= 1)/nrow(lumps)
round(perfect_reversion_pc_lumps * 100, 2)
# - 19.67%

sum(splits$perfect_reversion_count >= 1)
# - 24 splits
perfect_reversion_pc_splits = sum(splits$perfect_reversion_count >= 1)/nrow(splits)
round(perfect_reversion_pc_splits * 100, 2)
# - 25.81%

sum(splumps$reversion_count >= 1)
# - 85 splumps
reversion_pc_splumps = sum(splumps$reversion_count >= 1)/nrow(splumps)
round(reversion_pc_splumps * 100, 2)
# - splits: 39.53%

sum(splumps$perfect_reversion_count >= 1)
# - 48 splumps
perfect_reversion_pc_splumps = sum(splumps$perfect_reversion_count >= 1)/nrow(splumps)
round(perfect_reversion_pc_splumps * 100, 2)
# - 22.33%

#############################
#### CHANGE TRAJECTORIES ####
#############################

trajectories <- table(name_clusters$trajectory_lumps_splits)
sum(trajectories)
# - 862 name clusters

# TODO cleanup changes here

df_trajectories <- data.frame(trajectories)
df_trajectories

df_trajectories[df_trajectories$Var1 == "",]
# - 631 never corrected
round(631/sum(trajectories) * 100, 2)
# - 73.2%

(sum(trajectories) - 631)
# - 231 corrected

# first split: 
1 + 3 + 3 + 93 + 6 + 1 + 1 + 7 + 1 + 1 + 1
# - 118 splits first
118/sum(trajectories)

# split only
1 + 3 + 93 + 1
# - 97 splits only
97/118

# split -> lump
6 + 1 + 1 + 7 + 1
(6 + 1 + 1 + 7 + 1)/118

# - 16 split -> lump

# split -> lump only
6 + 1
7/118

# split -> split
1
1/118

# split -> lump -> split
7

# split -> lump -> lump -> split
2

# first lumped
45 + 7 + 2 + 1 + 1 + 54 + 3 + 3
# - 116 lumps first
116/sum(trajectories)

# lump only
45
45/116

# lump -> lump
7 + 2 + 1 + 1
(7 + 2 + 1 + 1)/116

# lump -> lump only
7

# lump -> split
54 + 3 + 3
(54 + 3 + 3)/116

# lump -> split only
54

# lump -> lump -> split
2 + 1 + 1

# lump -> lump -> split only
2

# lump -> lump -> split -> lump -> split
1 

# lump -> lump -> split -> split
1

# lump -> lump -> split -> lump -> split
1

# lump -> lump -> split -> split
1

# Changes not fully represented in two steps (i.e. something -> something -> ...)
2 + 1+1+3+1+1+7

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

lumps_with_perfect_reversions <- lumps[lumps$perfect_reversion_count >= 1,]
lumps_with_perfect_reversions
nrow(lumps_with_perfect_reversions)
# - 24
round(nrow(lumps_with_perfect_reversions)/nrow(lumps) * 100, 2)
# - 19.67%

perfect_lumps <- factor(lumps_with_perfect_reversions$perfect_reversions_summary)
table(perfect_lumps)

splits_with_perfect_reversions <- splits[splits$perfect_reversion_count >= 1,]
splits_with_perfect_reversions
nrow(splits_with_perfect_reversions)
# - 24
round(nrow(splits_with_perfect_reversions)/nrow(splits) * 100, 2)
# - 25.81%

perfect_splits <- factor(splits_with_perfect_reversions$perfect_reversions_summary)
table(perfect_splits)

# ALL TOGETHER NOW
perfect_splumps <- splumps[splumps$perfect_reversion_count >= 1,]
summary(perfect_splumps$type)
write.csv(data.frame(table(perfect_splumps$perfect_reversions_summary)), "tables/perfect_reversions_summary.csv")

# Count 'em
count_lump_split_lump <- 1
count_split_lump_split <- 2
count_lump_split <- 35
35/45
count_split_lump <- 10
10/45
count_total = count_lump_split_lump + count_split_lump_split + count_lump_split + count_split_lump

# BINOMIAL TEST: is there a 50-50 chance of lump_split vs split_lump?
binom.test(c(count_lump_split, count_split_lump))
# - 77.78%, p < 0.001

# FISHER'S EXACT TEST TIME
actual_lump_split <- count_lump_split
actual_split_lump <- count_split_lump
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
# - 215
summary(splumps$type)
# - lump: 122
# - splits: 93

lumps <- splumps[splumps$type == "lump",]
splits <- splumps[splumps$type == "split",]

nrow(lumps)
# - 122
nrow(splits)
# - 93

# How many splits revert previous lumps?
sum(splits$perfect_reversion_count > 0)
pc_splits_perfectly_reverting_lumps <- sum(splits$perfect_reversion_count > 0)/nrow(splits) * 100
round(pc_splits_perfectly_reverting_lumps, 2)
# 25.81%

splits_since_1980 <- splits[splits$year >= 1980,]
nrow(splits_since_1980)
# - 70 splits
splits_since_1980$year
# in range

sum(splits_since_1980$perfect_reversion_count > 0)
# - 13 splits
pc_splits_perfectly_reverting_lumps_since_1980 <- sum(splits_since_1980$perfect_reversion_count > 0)/nrow(splits_since_1980) * 100
round(pc_splits_perfectly_reverting_lumps_since_1980, 2)
# - 18.57%

splits_since_1950 <- splits[splits$year >= 1950,]
nrow(splits_since_1950)
# - 77 splits
splits_since_1950$year
# in range

sum(splits_since_1950$perfect_reversion_count > 0)
# - 17 splits
pc_splits_reverting_lumps_since_1950 <- sum(splits_since_1950$perfect_reversion_count > 0)/nrow(splits_since_1950) * 100
round(pc_splits_reverting_lumps_since_1950, 2)
# - 22.08%

#### Now with partial ####
sum(splits_since_1980$reversion_count > 0)
# - 24 splits
pc_splits_reverting_lumps_since_1980 <- sum(splits_since_1980$reversion_count > 0)/nrow(splits_since_1980) * 100
round(pc_splits_reverting_lumps_since_1980, 2)
# - 34.29%

####################################
#### PART 7. HIERARCHICAL MODEL ####
####################################

# Reload, just 'cos.
taxon_concepts <- read.csv("../taxon_concepts/list.csv")
nrow(taxon_concepts)
# - 1205 circumscriptions

name_clusters_all <- read.csv("../currently_recognized/list.csv")
nrow(name_clusters_all)
# - 1045 recognized names

# Duplicates?
sum(table(name_clusters_all$name) > 1)
# NO DUPLICATES!

# Now, let's get rid of the names added by aou_5_34
name_clusters <- name_clusters_all[name_clusters_all$taxon_concept_count > 0,]
nrow(name_clusters)
# 862

# Any extralimitals?
summary(name_clusters$order)
# YUP - 29 NAs! Eliminate.

name_clusters_no_extralimitals <- name_clusters[!is.na(name_clusters$order),]
nrow(name_clusters_no_extralimitals)
# 833

# How many eliminated?
862 - 833
# 29

# What's the mean definition count?
round(mean(name_clusters_no_extralimitals$taxon_concept_count), 3)
# - 1.366

sd(name_clusters_no_extralimitals$taxon_concept_count)
# 0.7316

summary(name_clusters_no_extralimitals$taxon_concept_count > 1)
# false: 630, true: 203, NA: 0

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
# - 833, yay

# Do we still see "hyemalis" as the toppermost of the poppermost?
name_clusters[which.max(name_clusters$taxon_concept_count),]$name
# - Yar.

# We shouldn't have any names with zero definitions.
sum(name_clusters_no_extralimitals$taxon_concept_count == 0)

# How many species have a single definition?
pc_single_defn <- sum(name_clusters_no_extralimitals$taxon_concept_count == 1)/nrow(name_clusters_no_extralimitals)
round(pc_single_defn * 100, 3)
# = 75.63%

# Okay, we need some higher taxonomy.
# And it's already in the file! Boom.
length(unique(factor(name_clusters_no_extralimitals$order)))
# - 25 orders
summary(is.na(name_clusters_no_extralimitals$order))
# - No NAs!

name_clusters_for_hierarchical_modeling <- name_clusters_no_extralimitals
nrow(name_clusters_for_hierarchical_modeling)
# - 833

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

# Get ready to STAN
#Sys.setenv(PATH=paste("C:\\Rtools\\bin", "C:\\Rtools\\mingw_64\\bin", Sys.getenv("PATH"), sep=";"))
#cat('Sys.setenv(BINPREF = "C:/Program Files/mingw_$(WIN)/bin/")',
#    file = file.path(Sys.getenv("HOME"), ".Rprofile"), 
#    sep = "\n", append = TRUE)
#Sys.getenv("PATH")
#system("where make")
#system("where g++")

library(rstan)
rstan_options(auto_write = TRUE)
options(mc.cores = parallel::detectCores())

# TODO: prior predictive modelling

# STAN model.
stan_d <- list(
    nobs = nrow(name_clusters_for_hierarchical_modeling),
    norder = length(levels(name_clusters_for_hierarchical_modeling$order)),
    nfamily = length(levels(name_clusters_for_hierarchical_modeling$family)),
    ngenus = length(levels(name_clusters_for_hierarchical_modeling$genus)),
    y = name_clusters_for_hierarchical_modeling$taxon_concept_count,
    order = as.integer(name_clusters_for_hierarchical_modeling$order),
    family = as.integer(name_clusters_for_hierarchical_modeling$family),
    genus = as.integer(name_clusters_for_hierarchical_modeling$genus),
    years_in_list = name_clusters_for_hierarchical_modeling$years_in_list
)

model_fit <- stan('counts_per_name_model.stan', data=stan_d, 
    control = list(
        adapt_delta = 0.9
        # 1: There were 1 divergent transitions after warmup. Increasing adapt_delta above 0.8 may help. See
        # http://mc-stan.org/misc/warnings.html#divergent-transitions-after-warmup 
    )
, iter=5000)
model_fit
# - Rhats appear to be at 1.0!
post <- rstan::extract(model_fit)

# Examine the results of the plot.
traceplot(model_fit, inc_warmup=T)

hist(post$sigma_i)
hist(post$sigma_j)
hist(post$sigma_k)

library("shinystan")
my_sso <- launch_shinystan(model_fit)
# - no Rhat above 1.1!

hist(post$lambda_0)
lambda_0 <- mean(post$lambda_0)
lambda_0
# = -4.47
exp(lambda_0)
# = 0.0114

1/exp(lambda_0)
# 87.3

# Order level changes
library(dplyr)

# We need to save this into files.
filename_postfix <- '_pre1982'

# Order measurements.
order_mean <- apply(post$pi_i, 2, mean)
order_interval_min <- apply(post$pi_i, 2, function(x) { quantile(x, probs=0.025) } )
order_interval_max <- apply(post$pi_i, 2, function(x) { quantile(x, probs=0.975) } )
order_interval_width <- order_interval_max - order_interval_min
count_per_order <- name_clusters_for_hierarchical_modeling %>% group_by(order) %>% summarize(count = length(id))
order_measurements <- data.frame(
    row.names=levels(count_per_order$order),
    count=count_per_order$count,
    min=order_interval_min,
    mean=order_mean,
    max=order_interval_max,
    significant=ifelse(((order_interval_min > 0 & order_interval_max > 0) == 1) | ((order_interval_min < 0 & order_interval_max < 0) == 1), "yes", "no"),
    interval_width=order_interval_width
)
order_measurements
write.csv(order_measurements, file=paste(sep="", 'tables/table_s4_hmod_order', filename_postfix, '.csv'))

# Plot
# count number of observations per order
#plot(order_interval_width ~ count_per_order$count, ylab="Interval width", xlab="Number of observations per order", main="5% credible interval widths for number of observations")

# FAMILY
family_mean <- apply(post$tau_j, 2, mean)
family_interval_min <- apply(post$tau_j, 2, function(x) { quantile(x, probs=0.025) } )
family_interval_max <- apply(post$tau_j, 2, function(x) { quantile(x, probs=0.975) } )
family_interval_width <- family_interval_max - family_interval_min
count_per_family <- name_clusters_for_hierarchical_modeling %>% group_by(family) %>% summarize(count = length(id))
family_measurements <- data.frame(
    row.names=levels(count_per_family$family),
    count=count_per_family$count,
    min=family_interval_min,
    mean=family_mean,
    max=family_interval_max,
    significant=ifelse(((family_interval_min > 0 & family_interval_max > 0) == 1) | ((family_interval_min < 0 & family_interval_max < 0) == 1), "yes", "no"),
    interval_width=family_interval_width
)
family_measurements
write.csv(family_measurements, file=paste(sep="", 'tables/table_s5_hmod_family', filename_postfix, '.csv'))

# Plot
# plot(family_interval_width ~ count_per_family$count, ylab="Interval width", xlab="Number of observations per family", main="5% credible interval widths for number of observations")

# GENUS
genus_mean <- apply(post$rho_k, 2, mean)
genus_interval_min <- apply(post$rho_k, 2, function(x) { quantile(x, probs=0.025) } )
genus_interval_max <- apply(post$rho_k, 2, function(x) { quantile(x, probs=0.975) } )
genus_interval_width <- genus_interval_max - genus_interval_min
count_per_genus <- name_clusters_for_hierarchical_modeling %>% group_by(genus) %>% summarize(count = length(id))
genus_measurements <- data.frame(
    row.names=levels(count_per_genus$genus),
    count=count_per_genus$count,
    min=genus_interval_min,
    mean=genus_mean,
    max=genus_interval_max,
    significant=ifelse(((genus_interval_min > 0 & genus_interval_max > 0) == 1) | ((genus_interval_min < 0 & genus_interval_max < 0) == 1), "yes", "no"),
    interval_width=genus_interval_width
)
genus_measurements
write.csv(genus_measurements, file=paste(sep="", 'tables/table_s6_hmod_genus', filename_postfix, '.csv'))

# Plot
plot(genus_interval_width ~ count_per_genus$count, ylab="Interval width", xlab="Number of observations per genus", main="5% credible interval widths for number of observations")

