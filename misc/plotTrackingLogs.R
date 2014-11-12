
data <- read.csv("trackingLog.csv", sep=";", header=T)

names(data) <- c("time", "- 1000ms", "+ 0ms", "+ 20ms", "+ 100ms", "ScanoutMidpoint")

data$time <- data$time - data$time[1]

plot(data$time, data$thisFrame, type='l')


plotRange <- function(data, from = NULL, to = NULL) {
  library("ggplot2")
  if (!is.null(from) && !is.null(to)) {
    data <- subset(data, time > from & time < to)
  }
  p <- ggplot(data, aes(x=time))
  
  #otherFeatures <- names(data)[-1]
  p <- p + geom_line(aes(y=past, colour="- 1000 ms"))
  p <- p + geom_line(aes(y=thisFrame, colour="+ 0 ms"))
  p <- p + geom_line(aes(y=thisFrame...20.ms, colour="+ 20 ms"))
  p <- p + geom_line(aes(y=thisFrame...100.ms, colour="+ 100 ms"))
  p <- p + geom_line(aes(y=ScanoutMidpoint, colour="ScanoutMidpoint"))
  p <- p + theme_light() #+ guide_legend(reverse = TRUE)
  plot(p)
}

plotRange <- function(data, from = NULL, to = NULL) {
  library("ggplot2")
  if (!is.null(from) && !is.null(to)) {
    data <- subset(data, time > from & time < to)
  }
  otherFeatures <- names(data)[-1]
  
  library(reshape2)
  newdata <- melt(data, id = "time")
  p <- ggplot(newdata, aes(x = time, y = value, color = variable)) + geom_line()
  p <- p + theme_light() #+ guide_legend(reverse = TRUE)
  plot(p)
}


from <- 1.05
to <- 1.2
zoom <- subset(data, time > from & time < to)

plot(zoom$time, zoom$thisFrame, type='l')

plotRange(data, 0, 3.5)







