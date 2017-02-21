package net.floodlightcontroller.crana.voting;

import java.io.InputStreamReader;
import java.util.Scanner;

public class Voting {
	public static int callVoting() {
		String path="D:\\vs project\\cplex_try2\\Release\\cplex_try2.exe";
        System.out.println("#*#*#  Voting is at your service");
        ProcessBuilder pb = new ProcessBuilder(path);//创建process管理者
        pb.redirectErrorStream(true);//子进程的输出都可以从标准output输出
        int exitid = 1;
        //读process输出的信息时 可能发生异常
        try {
            Process process = pb.start();//创建process
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