package core.Utils;

@SuppressWarnings("overloads")

//isnt LongSummaryStatistics
public class SimpleLongSummaryStatistics {
    private long max = Long.MIN_VALUE, min = Long.MAX_VALUE, sum, count, perSecond;

    public void add(long value) {
        perSecond++;
        sum += value;
    }

    private void calculate() {
        count++;
        min = Math.min(min, perSecond);
        max = Math.max(max, perSecond);
    }

    @Override
    public String toString() {
        calculate();
        String message = "per second - " + perSecond + ", max - " + max + ", min - " + min + ", average - " + (count > 0 ? (int) sum / count : 0.0d);
        perSecond = 0;

        return message;
    }
}