import java.io.IOException;
import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileInputStream;
import java.util.HashMap;

public class Project3{
    private static class PageData{// inner class holding info for each line
        int dirty_bit;
        long page_num;
        long last_accessed;// evict someone that has smallest line num (oldest one)
        int evict;//glag for eviction if process is not in file anymore
        long next_access;//

    }

    public ArrayList<StringBuilder> file;// define file
    public static void main(String args[]) throws IOException{
        // some error checking
        //System.out.println("hey main");
        if(args.length == 0){
            System.out.println("ERROR");
        } else {
            //System.out.println("in else");
            new Project3(args);
        }
    }//end main


    public Project3(String[] args) throws IOException {
        //define scanner for parsing info
        Scanner fileScan = new Scanner(new FileInputStream(args[8]));
        file = new ArrayList<>();
        
           /* equations:
            (n/(a+b))*a
            n - process0frames
            n = number offrames from cmd
            1:1 a = 1 and b =1
            */

        //initialize
        String a = "";
        String split = "";
        String splitA = "";
        String splitB = "";

        int a_mem = 0;
        int b_mem = 0;
        int n = 0;
        int p = 0;// Page size in KB
        a = (args[1]);// its gonna be lru or opt at this pos
        n = Integer.parseInt(args[3]);// pull the num Frames
        p = Integer.parseInt(args[5]);// get page size
        // get the split ratio and change it to ints for a and b
        split = (args[7]);
        splitA = Character.toString(split.charAt(0));
        splitB = Character.toString(split.charAt(2));
        a_mem = Integer.parseInt(splitA);
        b_mem = Integer.parseInt(splitB);

        //get a's and b's frames use a,b for memory split, n is num_frames
        int a_frames = (n/ (a_mem+b_mem)) * a_mem;
        int b_frames = (n/ (a_mem+b_mem)) * b_mem;

        //counter variables lru
        int lru_line_count = 0;
        int lru_hit = 0;
        int lru_page_faults = 0;
        int lru_disk_write = 0;
        //size of calculated frames two arrays for two processes
        PageData A [] = new PageData[a_frames];
        PageData B [] = new PageData[b_frames];

        //sanity check if my calculations are correct
        if(n != (a_frames + b_frames)){
            System.out.println("Number of pages doesnt match ");
            return;
        }
        //sanity check if user passer either lru or opt
        //-----------------------------------------------------------LRU -------------------------------
        if(a.equals("lru")){//case for LRU

            while (fileScan.hasNextLine())// get all of the lines
            {
                PageData data = new PageData();// define inner class
                //parse 3 things
                String line = fileScan.nextLine();
                String [] splitLine = line.split(" ");//split the adress and grab 3 values
                long whichProcess;
                long adressPreprocessed;
                whichProcess = Long.parseLong(splitLine[2]);// get int so its easier lol
                adressPreprocessed = Long.decode(splitLine[1]);
                //offset is log_2 of page size
                long adress = 0;
                int shift = 0;
                shift = (int)(Math.ceil(Math.log(p * 1024) / Math.log(2)));// log_2 but page size has to be in KB
                adress = adressPreprocessed >> shift;// hold shifted adress
                int flag_page_not_in = 0;

                lru_line_count += 1;// update line counter
                // ----------------------LRU A ----------------------------------------------------------
                if(whichProcess == 0) {// if first process A
                    int j = 0;
                    for (int i = 0; i < A.length; i++) {
                        // check if s to set dirty bit
                        if (A[i] == null)  {// not array not full
                            // do a page fault and put adress inside of array, put which line it was accessed
                            lru_page_faults++;
                            A[i] = new PageData();// set line if dirty bit on store

                            A[i].last_accessed = lru_line_count;// update last accessed line
                            A[i].page_num = adress;// set line number
                            if(splitLine[0].equals("s")){
                                A[i].dirty_bit = 1;
                            }else {
                                A[i].dirty_bit = 0;
                            }
                            break;
                        }
                        else if (A[i].page_num == adress) {// array full but page is in it
                            //check dirty bit
                            if(splitLine[0].equals("s")){
                                A[i].dirty_bit = 1;
                            }

                            A[i].last_accessed = lru_line_count;// update when last time accessed
                            lru_hit++;// hit count
                            break;
                        }
                        else if (j == A.length -1){// set flag for eviction case
                            flag_page_not_in = 1;
                            break;
                        }
                        j++;

                    }//end for loop

                    if(flag_page_not_in == 1 ) {// array full and page not in it
                        // evict oldest process from array and replace it with new one
                        long to_replace = 2147483647;// set as max int
                        int index = 0;
                        for (int i = 0; i < A.length; i++) {// iterate over and find smallest process
                            if(A[i].last_accessed < to_replace ){// find smallest line(oldest process)
                                to_replace = A[i].last_accessed;
                                index = i;// update index
                            }
                        }//end for
                        if(A[index].dirty_bit == 1){// f dirty bit is set you have to write tot the file
                            lru_disk_write++;
                           
                        }
                        //Evict the line at A[index], so change last accessed, adress, page fault
                        A[index].last_accessed = lru_line_count;
                        A[index].page_num = adress;
                        lru_page_faults++;

                        if(splitLine[0].equals("l")){// if load then set new dirty bit to 0 else leave it at 1
                            A[index].dirty_bit = 0;
                        }
                        else{
                            A[index].dirty_bit = 1;
                        }
                    }
//----------------- LRU B ------------------------------------------------------------------------------------------------
                }else {// process is B so mirror of A process
                    int j = 0;
                    for (int i = 0; i < B.length; i++) {
                        // check if s to set dirty bit
                        if (B[i] == null)  {// not array not full
                            // do a page fault and put adress inside of array, put which line it was accessed
                            lru_page_faults++;
                            B[i] = new PageData();// set line if dirty bit on store

                            B[i].last_accessed = lru_line_count;// update last accessed line
                            B[i].page_num = adress;// set line number
                            if(splitLine[0].equals("s")){
                                B[i].dirty_bit = 1;
                            }else {
                                B[i].dirty_bit = 0;
                            }
                            break;
                        }
                        else if (B[i].page_num == adress) {// array full but page is in it
                            //check dirty bit
                            if(splitLine[0].equals("s")){
                                B[i].dirty_bit = 1;
                            }
                            B[i].last_accessed = lru_line_count;// update when last time accessed
                            lru_hit++;// hit count
                            break;
                        }
                        else if (j == B.length -1){// set flag for eviction case
                            flag_page_not_in = 1;
                            break;
                        }
                        j++;

                    }//end for loop

                    if(flag_page_not_in == 1 ) {// array full and page not in it
                        // evict oldest process from array and replace it with new one
                        long to_replace = 2147483647;// set as max int
                        int index = 0;
                        for (int i = 0; i < B.length; i++) {// iterate over and find smallest line(oldest process)
                            if(B[i].last_accessed < to_replace ){
                                to_replace = B[i].last_accessed;
                                index = i;// update index
                            }
                        }//end for
                        if(B[index].dirty_bit == 1){//dirty bit is set you have to write to the file
                            lru_disk_write++;
                        }
                        //Evict the line at A[index], so change last accessed, adress, page fault
                        B[index].last_accessed = lru_line_count;
                        B[index].page_num = adress;
                        lru_page_faults++;

                        if(splitLine[0].equals("l")){// if load then set new dirty bit to 0 else leave it at 1
                            B[index].dirty_bit = 0;
                        }
                        else{
                            B[index].dirty_bit = 1;
                        }
                    }
                }
            }
            fileScan.close();
        }//end LRU if
        else {// user entered invalid algorithm
            System.out.println("INVALID PAGE REPLACEMENT ALGORITHM, BYE!");
            return;
        }
        //---------------------------------------------------------------------------------print section
        if(a.equals("lru")){
            System.out.println("Algorithm: LRU");
            System.out.println("Number of frames: " + n);
            System.out.println("Page size: " + p + " KB");
            System.out.println("Total memory accesses: " + lru_line_count);
            System.out.println("Total page faults: " + lru_page_faults);
            System.out.println("Total writes to disk: " + lru_disk_write);
        }
    }//end page replacement function
} //end PageReplacement class

