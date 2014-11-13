
data <- read.csv("trackingLog.csv", sep=";", header=T)

names(data) <- c("time", "tft - 1000ms", "tft", "tft + 10ms", "tft + 50ms", "ScanoutMidpoint")

# subtract initial time
data$time <- data$time - data$time[1]

# some plotting theme settings
theme_customized <- function() {
  theme(axis.title=element_text(family="Ubuntu", face="bold", color="#CC6600", size=16),
        axis.text=element_text(family="Monospace", face="plain", color="blue", size=14),
        legend.title=element_text(family="Ubuntu", face="bold", color="black", size=16),
        legend.text=element_text(family="Ubuntu", face="plain", color="#111111", size=14))  
}


# create the prediction + delta plot
plotRange <- function(data, from = NULL, to = NULL, filename = NULL) {
  library("ggplot2")
  if (!is.null(from) && !is.null(to)) {
    data <- subset(data, time > from & time < to)
  }
  data$time <- data$time * 1000
  data$time <- data$time - data$time[1]
  
  library(reshape2)
  newdata <- melt(data, id = "time")
  p1 <- ggplot(newdata, aes(x = time, y = value, color = variable)) +
    geom_line() +
    geom_point(shape=19) +
    xlab("time [ms]") +
    ylab("yaw [°]") +
    labs(colour="Predictions") +
    theme_light() + 
    theme_customized()
    
  plot(p1)
  if (!is.null(filename)) {
    ggsave(paste(filename, "_yaw.png", sep=''), p1, type='cairo-png', dpi=72)
  }
  
  dt <- data$time[-1] - data$time[-length(data$time)]
  df <- data.frame(time=data$time[-length(data$time)], dt=dt)
  p2 <- ggplot(df, aes(x = time, y = dt)) +
    geom_line() +
    geom_point(shape=19) +
    xlab("time [ms]") +
    ylab("delta [ms]") +
    theme_light() + 
    theme_customized()
  
  plot(p2)
  if (!is.null(filename)) {
    ggsave(paste(filename, "_dt.png", sep=''), p2, type='cairo-png', dpi=72)
  }
  
  print(mean(dt))
}


plotRange(data)






