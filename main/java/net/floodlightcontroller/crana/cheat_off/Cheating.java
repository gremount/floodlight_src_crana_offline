package net.floodlightcontroller.crana.cheat_off;

import java.io.InputStreamReader;
import java.util.Scanner;

public class Cheating {
	public static int callCheat() {
        String path = "/Users/ningjieqian/Library/Developer/Xcode/DerivedData/cheat_off-gbtvxpzqobiogshhiafkwojfcnnf/Build/Products/Debug/cheat_off";
        System.out.println("#*#*#  Cheating is at your service");
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
