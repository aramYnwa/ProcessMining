package com.company.feature_extraction.encoding;

import com.company.xlog.XLogHandler;
import java.util.Date;
import java.util.HashMap;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class XLogManager {

  XLog xLog;
  Long logAveragePerformance;
  HashMap<XTrace, Double> traceLabelMap = null;

  public XLogManager (XLog xLog) {
    this.xLog = xLog;
    this.logAveragePerformance = calculateAverageTime(xLog);
    labelTraces();
  }

  /**
   * Function for labeling all traces.
   */
  private void labelTraces() {
    traceLabelMap = new HashMap<>();
    for (XTrace trace : this.xLog){
      Double label = classifyTrace(trace);
      traceLabelMap.put(trace, label);
    }
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
   *
   * @param trace
   * @return
   */
  private Double classifyTrace (XTrace trace) {
    long tracePerformance = getTraceDuration(trace);
    if (tracePerformance > logAveragePerformance)
      return Double.valueOf("1");
    else
      return Double.valueOf("0");
  }

  /**
   * For a certain trace returns map of events and their frequency number.
   * @param xTrace
   * @return
   */
  HashMap<String, Integer> getEventFrequencyMap (XTrace xTrace) {
    HashMap<String, Integer> map = new HashMap<>();
    for (XEvent event : xTrace) {
      String eventName = XConceptExtension.instance().extractName(event);
      if (!map.containsKey(eventName)) {
        map.put(eventName, 1);
      } else {
        Integer value = map.get(eventName);
        map.put(eventName, value + 1);
      }
    }
    return map;
  }

  public XLog getxLog() {
    return xLog;
  }

  public HashMap<XTrace, Double> getTraceLabelMap() {
    return traceLabelMap;
  }
}
