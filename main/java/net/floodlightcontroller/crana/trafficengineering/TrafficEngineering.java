package net.floodlightcontroller.crana.trafficengineering;

import java.io.InputStreamReader;
import java.util.Scanner;

public class TrafficEngineering {
	public static int callTE() {
		String path="D:\\TE\\Release\\TE.exe";
        System.out.println("#*#*#  Traffic Engineering is at your service");
        ProcessBuilder pb = new ProcessBuilder(path);//ProcessBuilder -> This class is used to create operating system processes. 
        pb.redirectErrorStream(true);
        int exitid = 1;
        try {
            Process process = pb.start();
            //getInputStream -> Returns the input stream connected to the normal output of the subprocess.
            Scanner input = new Scanner(new InputStreamReader(process.getInputStream()));
            while(input.hasNext()){
            	System.out.println(input.nextLine());
            }
            exitid = process.waitFor();
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
  
        return exitid;
    }
}