package org.xena.analysis;

import java.util.TreeMap;

public class TpmStat  {

  int count;
  double oldMean, newMean, oldStdev, newStdev;

  TpmStat() {
    count = 0;
  }

  void clearCount()
  {
    count = 0;
  }

  void addStat(double x)
  {
    count++;

    // See Knuth TAOCP vol 2, 3rd edition, page 232
    if (count == 1)
    {
      oldMean = newMean = x;
      oldStdev = 0.0;
    }
    else
    {
      newMean = oldMean + (x - oldMean)/ count;
      newStdev = oldStdev + (x - oldMean)*(x - newMean);

      // set up for next iteration
      oldMean = newMean;
      oldStdev = newStdev;
    }
  }

  int numDataValues()
  {
    return count;
  }

  double mean()
  {
    return (count > 0) ? newMean : 0.0;
  }

  double variance()
  {
    return ( (count > 1) ? newStdev /(count - 1) : 0.0 );
  }

  double standardDeviation()
  {
    return Math.sqrt( variance() );
  }

  @Override
  public String toString() {
    return "{" +
      "count:" + count +
//      ", oldMean:" + oldMean +
      ", mean:" + newMean +
//      ", oldStdev:" + oldStdev +
      ", stdev:" + newStdev +
      ", variance:" + this.variance()+
      ", stdev:" + this.standardDeviation()+
      '}';
  }
}
