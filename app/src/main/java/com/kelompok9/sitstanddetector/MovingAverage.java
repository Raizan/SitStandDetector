package com.kelompok9.sitstanddetector;

import java.util.LinkedList;
import java.util.Queue;

public class MovingAverage {
    private final Queue<Float> window = new LinkedList<Float>();
    private final int period;
    private float sum;

    public MovingAverage(int period) {
        assert period > 0 : "Period must be a positive integer";
        this.period = period;
    }

    public void newNum(float num) {
        sum += num;
        window.add(num);
        if (window.size() > period) {
            sum -= window.remove();
        }
    }

    public float getAvg() {
        if (window.isEmpty()) return 0; // technically the average is undefined
        return sum / window.size();
    }

//    public static void main(String[] args) {
//        double[] testData = {1,2,3,4,5,5,4,3,2,1};
//        int[] windowSizes = {3,5};
//        for (int windSize : windowSizes) {
//            MovingAverage ma = new MovingAverage(windSize);
//            for (double x : testData) {
//                ma.newNum(x);
//                System.out.println("Next number = " + x + ", SMA = " + ma.getAvg());
//            }
//            System.out.println();
//        }
//    }
}