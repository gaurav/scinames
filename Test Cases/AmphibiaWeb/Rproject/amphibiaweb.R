getwd()
name_stability <- read.csv("../name_stability/name_stability_initial.csv")

#### Overall nums ####
first <- name_stability[1,]
last <- name_stability[nrow(name_stability),]
(last$count_binomial - first$count_binomial)
# - +575 species
(last$count_binomial - first$count_binomial)/first$count_binomial
# - 8.2%
(last$count_genera - first$count_genera)
# - +11 genera
(last$count_genera - first$count_genera)/first$count_genera
# - 2.0%

# Last to first stability
last$names_identical_to_first_pc_union
# - 82.75%

last$names_identical_to_first
# - 6635

# names not identical 
last$count_binomial - last$names_identical_to_first
# 979 names

# Differences with prev
min(name_stability$names_identical_to_prev_pc_union, na.rm = T)
min(name_stability$names_identical_to_prev_pc_this, na.rm = T)

#### Different synonymy algorithms ####
synonymy_by_id <- read.csv("../synonymy/renames_using_changes.csv")
head(synonymy_by_id)
nrow(synonymy_by_id)
# - 496 synonyms

# TODO: unsort direction
which(table(paste(synonymy_by_id$From, " -> ", synonymy_by_id$To)) > 1)
length(unique(paste(synonymy_by_id$From, " -> ", synonymy_by_id$To)))
# - 491 unique synonyms

synonymy_by_all_data <- read.csv("../synonymy/renames_using_all_data.csv")
head(synonymy_by_all_data)
nrow(synonymy_by_all_data)
# - 987 synonyms

length(which(table(paste(synonymy_by_all_data$From, " -> ", synonymy_by_all_data$To)) > 1))
length(unique(paste(synonymy_by_all_data$From, " -> ", synonymy_by_all_data$To)))
# - 930 unique synonyms

synonymy_by_column <- read.csv("../synonymy/renames_using_synonymy_column.csv")
head(synonymy_by_column)
nrow(synonymy_by_column)
# - 3,670 synonyms

length(which(table(paste(synonymy_by_column$From, " -> ", synonymy_by_column$To)) > 1))
# - 139 duplicates
unique(paste(synonymy_by_column$From, " -> ", synonymy_by_column$To))
length(unique(paste(synonymy_by_column$From, " -> ", synonymy_by_column$To)))
# - 140 unique synonyms

#### Overall changes ####
changes_by_name <- read.csv("../overall_changes/changes_by_name_only.csv")
nrow(changes_by_name)
summary(changes_by_name$Dataset)
# 979 in Jan 2017, 404 in Oct 2012

changes_by_name_cluster <- read.csv("../overall_changes/changes_by_name_cluster.csv")
nrow(changes_by_name_cluster)
summary(changes_by_name_cluster$Dataset)
# 635 added, 4 deleted

#### With synonymy ####
name_stability <- read.csv("../name_stability/name_stability_synonyms_from_id_and_synonym_field.csv")

# Visualization take 2: sparklines of recognized species and additions and deletions.
library(zoo)
name_stability$date_zoo <- as.Date(name_stability$date)

skip_first <- 2:length(name_stability$date)
plot(
  type="l",
  name_stability$species_added[skip_first] ~ name_stability$date_zoo[skip_first]
)

plot(
  type="l",
  name_stability$species_deleted[skip_first] ~ name_stability$date_zoo[skip_first]
)

plot(
  type="l",
  (name_stability$species_added[skip_first] + name_stability$species_deleted[skip_first]) ~ name_stability$date_zoo[skip_first]
)

plot(
  type="l",
  cumsum(name_stability$species_added[skip_first] + name_stability$species_deleted[skip_first]) ~ name_stability$date_zoo[skip_first]
)


plot(
  type="l",
  name_stability$count_binomial ~ name_stability$date_zoo
)

plot(
  type="l",
  name_stability$count_binomial ~ name_stability$date_zoo,
  ylim=c(0, max(name_stability$count_binomial))
)

# Trying Rob's idea
skip_two <- 3:length(name_stability$date)
temp <- name_stability$clusters_identical_to_prev_pc_union
temp

temp[1] <- 100
temp

plot(
  type="l",
  temp ~ name_stability$date_zoo,
  ylim=c(min(name_stability$clusters_identical_to_prev_pc_union, na.rm=T), 100),
  ylab="% identical name clusters",
  main="Name clusters relative to previous checklist"
)

temp2 <- name_stability$names_identical_to_prev_pc_union
temp2[1] <- 100
temp2

plot(
  type="l",
  temp2 ~ name_stability$date_zoo,
  ylim=c(min(temp2, na.rm=T), 100),
  ylab="% identical names",
  main="Names relative to previous checklist"
)

barplot(
  temp2 - 100, names=name_stability$date_zoo,
  ylab="% identical names below 100%",
  main="Names relative to previous checklist"
)

plot(
  type="l",
  name_stability$clusters_identical_to_first_pc_union ~ name_stability$date_zoo,
  ylim=c(min(name_stability$clusters_identical_to_first_pc_union, na.rm=T), 100),
  ylab="% identical name clusters",
  main="Name clusters relative to first checklist"
)

barplot(
  name_stability$names_identical_to_first_pc_union - 100, names=name_stability$date_zoo,
  ylab="% identical names below 100%",
  main="Names relative to first checklist"
)

barplot(
  name_stability$clusters_identical_to_first_pc_union - 100, names=name_stability$date_zoo,
  ylab="% identical name clusters below 100%",
  main="Name clusters relative to first checklist"
)

# Final plot!

# 1676x501
#install.packages("Cairo")
library(Cairo)

Cairo(file="../name_stability/name_and_name_clusters_relative_to_first.png",
    type="png",
    units="in",
    dpi=120,
    width=8,
    height=4.5
)
plot(
  type="l",
  name_stability$names_identical_to_first_pc_union ~ name_stability$date_zoo,
  ylim=c(min(name_stability$names_identical_to_first_pc_union, na.rm=T), 100),
  lty=2,
  ylab="% identical names/name clusters",
  xlab="Date",
  main="Name and name cluster identity relative to first checklist"
)

lines(
  name_stability$clusters_identical_to_first_pc_union ~ name_stability$date_zoo,
  lty=1
)

legend("bottomleft",
  lty=c(1, 2),
  legend=c("Name clusters identical to first checklist", "Names identical to first checklist")
)
dev.off()

# Visualization!
recognition_percent <- matrix(c(
  name_stability$previously_recognized_100pc,
  name_stability$previously_recognized_90pc,
  name_stability$previously_recognized_80pc,
  name_stability$previously_recognized_70pc,
  name_stability$previously_recognized_60pc,
  name_stability$previously_recognized_50pc,
  name_stability$previously_recognized_40pc,
  name_stability$previously_recognized_30pc,
  name_stability$previously_recognized_20pc,
  name_stability$previously_recognized_10pc,
  name_stability$previously_recognized_0pc
), ncol=11)
recognition_percent

recognition_percent[is.na(recognition_percent)] <- 0

colfunc <- colorRampPalette(c("red","springgreen","royalblue"))

barplot(
  main=paste("AmphibiaWeb 'previous recognition percentage' plot (", name_stability$date[1], " to ", name_stability$date[length(name_stability$date)], ")", sep=""),
  t(recognition_percent),
  names.arg=name_stability$date,
  col=colfunc(11)        
)
legend(
  "bottomright",
  col=colfunc(11),
  pch=15,
  legend = c(
    "100%", "90%", "80%", "70%", "60%", "50%", "40%", "30%", "20%", "10%", "0%"
  )
)

max(t(recognition_percent))



stable_always <- t(recognition_percent)[1,]
min(stable_always[2:length(stable_always)])
# - 6593

c(min(stable_always[2:length(stable_always)]),max(colSums(t(recognition_percent))))

barplot(
  main=paste("AmphibiaWeb 'previous recognition percentage' plot (", name_stability$date[1], " to ", name_stability$date[length(name_stability$date)], ")", sep=""),
  t(recognition_percent),
  names.arg=name_stability$date,
  col=colfunc(11),
  ylim=c(min(stable_always[2:length(stable_always)]),max(colSums(t(recognition_percent))))
)
legend(
  "bottomright",
  col=colfunc(11),
  pch=15,
  legend = c(
    "100%", "90%", "80%", "70%", "60%", "50%", "40%", "30%", "20%", "10%", "0%"
  )
)

recognition_percent_skip_first <- t(recognition_percent)[2:11,]
recognition_percent_skip_first[,2:52]

barplot(
  main=paste("AmphibiaWeb 'previous recognition percentage' plot (", name_stability$date[1], " to ", name_stability$date[length(name_stability$date)], ")", sep=""),
  recognition_percent_skip_first[,2:52],
  names.arg=name_stability$date[2:52],
  col=colfunc(11)
)
legend(
  "bottomright",
  col=colfunc(11),
  pch=15,
  legend = c(
    "100%", "90%", "80%", "70%", "60%", "50%", "40%", "30%", "20%", "10%", "0%"
  )
)

#### Messing around with CITES ####
setwd("../../CITES")
getwd()
cites <- read.csv("Index_of_CITES_Species_2017-06-12 13-47.csv")
length(cites$TaxonId)
length(unique(cites$TaxonId))

cites_amphibia <- cites[cites$Class == "Amphibia",]
write.csv(cites_amphibia, "amphibia.csv")

nrow(cites_amphibia)
#  - 181 records
summary(cites_amphibia$RankName)
# 17 genera, 164 species
summary(cites_amphibia$CurrentListing)
# - I: 28, II: 146, II/NC: 1, III: 4, NC: 2

cites_amphibia$FullName

cites_reptilia <- cites[cites$Class == "Reptilia",]
write.csv(cites_reptilia, "reptilia.csv")

nrow(cites_reptilia)
# - 1018 records

summary(cites_reptilia$RankName)
# 12 families, 109 genera, 1 order, 891 species, 5 subspecies.
summary(cites_reptilia$CurrentListing)
# - I: 106, I/II: 36, I/III: 1, I/NC: 1, II: 807, III: 67

# Compare to amphibiaweb
amphibiaweb_last <- read.csv("../AmphibiaWeb/taxonomy-archive-master/amphib_names_20170101.tsv", sep="\t")
nrow(amphibiaweb_last)
amphibiaweb_last$sciname <- paste(amphibiaweb_last$genus, amphibiaweb_last$species) 
head(amphibiaweb_last$sciname)

# Try a merge for amphibia
merge <- merge(cites_amphibia, amphibiaweb_last, by.x="FullName", by.y="sciname")
nrow(merge)
# - 161 out of 164
161/164
# - 98.17%

# Compare to amphibiaweb_first
amphibiaweb_first <- read.csv("../AmphibiaWeb/taxonomy-archive-master/amphib_names_20121001.tsv", sep="\t")
nrow(amphibiaweb_first)
amphibiaweb_first$sciname <- paste(amphibiaweb_first$genus, amphibiaweb_first$species) 
head(amphibiaweb_first$sciname)

# Try a merge for amphibia
merge <- merge(cites_amphibia, amphibiaweb_first, by.x="FullName", by.y="sciname")
nrow(merge)
# - 128 out of 164
157/164
# - 95.7%


# And with SciNames?
amphibiaweb_scinames <- read.csv("../../CITES/amphibia_from_scinames.csv")
nrow(amphibiaweb_scinames)
summary(amphibiaweb_scinames$first_added_dataset)
amphibiaweb_scinames[which(is.na(amphibiaweb_scinames$uri.guid)),]

# Compare to Reptile Database
reptiledb <- read.csv("../Reptile Database/CSVs/reptile_checklist_2016_12.csv")
nrow(reptiledb)
# - 10,501 rows

# Try a merge for reptile database
merge_reptiledb <- merge(cites_reptilia, reptiledb, by.x="FullName", by.y="Species")
nrow(merge_reptiledb)
# - 824 out of 891 species
824/891
# - 92.48%

