getwd()
name_stability <- read.csv("../name_stability/name_stability.csv")

head(name_stability)

first <- name_stability[1,]
last <- name_stability[nrow(name_stability),]
(last$count_binomial - first$count_binomial)
(last$count_binomial - first$count_binomial)/first$count_binomial
(last$count_genera - first$count_genera)
(last$count_genera - first$count_genera)/first$count_genera

# Visualization take 2: sparklines of recognized species and additions and deletions.
library(zoo)
name_stability$date_formatted <- paste(name_stability$date, "-01", sep="")
name_stability$date_formatted[8] <- "2016-01-17"
name_stability$date_zoo <- as.Date(name_stability$date_formatted)

name_stability$names_added
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

library(Cairo)

Cairo(file="../name_stability/name_and_name_clusters_relative_to_first.png",
      type="png",
      units="in",
      dpi=200,
      width=8,
      height=4.5
)

plot(
  type="l",
  name_stability$clusters_identical_to_first_pc_union ~ name_stability$date_zoo,
  ylim=c(min(name_stability$clusters_identical_to_first_pc_union, na.rm=T), 100),
  ylab="% identical names",
  main="Names relative to first checklist"
)

dev.off()

# Max
min(name_stability$clusters_identical_to_first_pc_union)

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

name_stability$names_identical_to_first
name_stability$names_identical_to_first_pc_union
name_stability$clusters_identical_to_first

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
