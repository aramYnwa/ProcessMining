package com.company.feature_extraction.encoding;

import com.company.xlog.XLogHandler;
import java.util.Date;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class XLogManager {

  XLog xLog;
  Long logAveragePerformance;

  public XLogManager (XLog xLog) {
    this.xLog = xLog;
    logAveragePerformance = calculateAverageTime(xLog);
  }

  /**
   * Function calculates average performance time of process log.
   * @param xlog
   * @return
   */
  private Long calculateAverageTime(XLog xlog) {
    long accumulator = 0;
    long temp = 0;
    for (XTrace trace : xlog) {
      long seconds = getTraceDuration(trace);
      accumulator += seconds;
      temp++;

    }
    return accumulator / temp;
  }

  /**
   * Function for certain trace  returns
   * trace time performance measured by days.
   * @param trace
   * @return
   */
  public long getTraceDuration(XTrace trace) {
    int length = trace.size();
    XEvent firstEvent = trace.get(0);
    Date firstEventTS = XTimeExtension.instance().extractTimestamp(firstEvent);

    XEvent lastEvent = trace.get(length - 1);
    Date lastEventTS = XTimeExtension.instance().extractTimestamp(lastEvent);

    //Getting difference by days.
    long days = (lastEventTS.getTime() - firstEventTS.getTime()) / (1000 * 60 * 60 * 24);

    return days;
  }

  /**
   * For each trace function returns
   *  "1" if given trace is variant
   *  "0" if given trace is normal
   * @param trace
   * @return
   */
  public Double classifyTrace (XTrace trace) {
    long tracePerformance = getTraceDuration(trace);
    if (tracePerformance > logAveragePerformance)
      return Double.valueOf("1");
    else
      return Double.valueOf("0");
  }
}
