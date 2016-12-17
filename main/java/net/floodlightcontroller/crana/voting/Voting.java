package net.floodlightcontroller.crana.voting;

import java.io.InputStreamReader;
import java.util.Scanner;

public class Voting {
	public static int callVoting() {
		String path="D:\\vs project\\cplex_try2\\Release\\cplex_try2.exe";
        System.out.println("#*#*#  Voting is at your service");
        ProcessBuilder pb = new ProcessBuilder(path);
        pb.redirectErrorStream(true);
        int exitid = 1;
        try {
            Process process = pb.start();
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
