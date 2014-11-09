data1 <- read.csv("trackingLog1.csv", sep=";", header=F)
data2 <- read.csv("trackingLog2.csv", sep=";", header=F)
data3 <- read.csv("trackingLog3.csv", sep=";", header=F)

names(data1) <- c("time", "yaw")
names(data2) <- c("time", "yaw")
names(data3) <- c("time", "yaw")

#plot(data$time, data$yaw, type='l')

from <- 0
to <- 9912999
zoom1 <- subset(data1, time > from & time < to)
zoom2 <- subset(data2, time > from & time < to)
zoom3 <- subset(data3, time > from & time < to)

#par(new=T)
plot(zoom1$time, zoom1$yaw, type='l')
#par(new=F)
lines(zoom2$time, zoom2$yaw, type='l')
lines(zoom3$time, zoom3$yaw, type='l')
