package net.floodlightcontroller.crana.cheat_on;

import java.io.InputStreamReader;
import java.util.Scanner;

public class Cheating_on {
	public static int callCheat_on() {
        String path = "/Users/ningjieqian/Library/Developer/Xcode/DerivedData/cheat_off-bbqltodvgfikutdkftqnotgpflwd/Build/Products/Debug/cheat_on";
        ProcessBuilder pb = new ProcessBuilder(path);
        pb.redirectErrorStream(true);
        int exitid = 1;
        try {
            Process process = pb.start();
            Scanner input = new Scanner(new InputStreamReader(process.getInputStream()));
            while(input.hasNext()){
            	System.out.println(input.nextLine());
            }
            exitid = process.waitFor(); //正常结束，返回0
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
  
        return exitid;
    }
}
