package com.motadata.nms.practice;

public class TestPrime {
  public static void main(String[] args) {
    System.out.println("ANWER:"+ new PrimeGenerator().getTillThis(Integer.parseInt(args[0])));
  }

  static class PrimeGenerator{

    public long getTillThis(int max){
      long totalSum = 0;
      for(int i=1;i<=max;i++){
        totalSum += getSum(i);
      }
      return totalSum;
    }

    public long getSum(int n){
      long sum = 0;
      for(int i=1;i<=n;i++){
        sum += i;
      }
      return sum;
    }
  }
}
