package net.floodlightcontroller.crana.selfishrouting;

import java.io.InputStreamReader;
import java.util.Scanner;

public class SelfishRouting {
	public static int callSR() {
        String path = "/Users/ningjieqian/Library/Developer/Xcode/DerivedData/SR-gdmfrtbsvprfkwgpsylzcregezdq/Build/Products/Debug/SR";
        System.out.println("#*#*#  Selfish Routing is at your service");
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
