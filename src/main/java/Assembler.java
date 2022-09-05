import java.io.*;
import java.util.Scanner;

public class Assembler {

    /*
    public static void main(String[] args) {
        replace_pseudo("Program");
        demangle_identifiers("Program");
        read_assembly("Program");
    }
    */

    public static void replace_pseudo(String Program){
        try {
            File myObj = new File("Programs/" + Program + ".txt");
            FileWriter myWriter = new FileWriter("Programs/Intermediates/PreAssembly/" + Program + "_PreAssembly.txt");
            Scanner myReader = new Scanner(myObj);

            String addedData = "";
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();

                if(data.contains(".") && data.contains(":")) {
                    addedData = " //" + data;
                }else if(pseudo_instructions(data)) {
                    if(data.contains("j")){
                        myWriter.write("li x1, " + data.substring(data.indexOf(".")) + addedData + "\n");
                        addedData = "";
                    }
                    /*
                    else if(data.contains("loadFir")){
                        int immediate = Integer.parseInt(data.substring(data.indexOf(",") + 2));

                        if(immediate > 1024){
                            int lower = immediate & 0b000000000111111111;
                            int upper = immediate & 0b111111111000000000;;

                            myWriter.write("li x5, " + lower + addedData + "\n");
                            myWriter.write("lui x5, " + (upper >> 9) + "\n");
                        }else{
                            myWriter.write("li x5, " + immediate + addedData + "\n");
                        }

                        String memorypos = data.substring(0, data.indexOf(",")).replaceAll(("[^0-9]"), "");
                        myWriter.write("swi x5, " + (Integer.parseInt(memorypos) + 1983) + "\n");
                        addedData = "";
                    }
                    */
                    else if(data.contains("li")){

                        int immediate = 0;

                        if(data.contains("b")){
                            immediate = Integer.parseInt(data.substring(data.indexOf("b") + 1).replace(" ", ""), 2);
                        }else{
                            immediate = Integer.parseInt(data.substring(data.indexOf(",") + 2).replace(" ", ""));
                        }

                        int rd_index = data.indexOf("x");
                        int rd = find_register(data.substring(rd_index, rd_index + 3).replace(" ", ""));

                        if(immediate > 4096){
                            int lower = immediate & 0b000000000000111111111111;
                            int upper = immediate & 0b111111111111000000000000;
                            /*
                            System.out.println(immediate);
                            System.out.println(lower);
                            System.out.println(upper >> 9);
                            */

                            if(data.contains("v")){
                                myWriter.write("vli x" + rd + ", " + lower + addedData + "\n");
                                myWriter.write("vlui x" + rd + ", " +  (upper >> 12) + "\n");
                            }else{
                                myWriter.write("li x" + rd + ", " + lower + addedData + "\n");
                                myWriter.write("lui x" + rd + ", " +  (upper >> 12) + "\n");
                            }

                            addedData = "";
                        }else{

                            if(data.contains("v")){
                                myWriter.write("vli x" + rd + ", " + immediate + addedData + "\n");
                            }else{
                                myWriter.write("li x" + rd + ", " + immediate + addedData + "\n");
                            }

                            addedData = "";
                        }
                    }else if((data.contains("sw") && data.contains("swi")) || (data.contains("lw") && data.contains("lwi") && !data.contains("v"))){
                        myWriter.write(data + ", x0" + addedData + "\n");
                        addedData = "";
                    }
                }else if(data != ""){
                    myWriter.write(data + addedData + "\n");
                    addedData = "";
                }
            }
            myWriter.close();
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static boolean pseudo_instructions(String instruction) {

        if (instruction.contains("j") || instruction.contains("loadFir") || instruction.contains("li") || instruction.contains("sw") || instruction.contains("lw")) {
            return true;
        }
        return false;
    }


    public static void demangle_identifiers(String Program){
        String[] functionarray = new String[10];

        int functionarray_index = 0;
        int[] functionaddress = new int[10];


        try {
            File myObj = new File("Programs/Intermediates/PreAssembly/" + Program + "_PreAssembly.txt");
            Scanner myReader = new Scanner(myObj);

            int instruction_address = 0;

            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();

                if(data.contains("//")){
                    functionarray[functionarray_index] = data.substring(data.indexOf("/") + 2,data.indexOf(":") - 1);
                    functionaddress[functionarray_index] = instruction_address;

                    functionarray_index += 1;
                }
                instruction_address += 1;
            }

            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        try {
            File myObj = new File("Programs/Intermediates/PreAssembly/" + Program + "_PreAssembly.txt");
            FileWriter myWriter = new FileWriter("Programs/Intermediates/Assembly/" + Program + "_Assembly.txt");
            Scanner myReader = new Scanner(myObj);

            int instruction_address = 0;

            while(myReader.hasNextLine()){
                String data = myReader.nextLine();

                if(data.contains("//")){
                    data = data.replace(data.substring(data.indexOf("/")), "");
                }else{
                    for(int i = 0; i < functionarray.length; i++){
                        if(functionarray[i] != null){
                            if(data.contains(functionarray[i])){
                                data = data.replace(data.substring(data.indexOf("."),data.length()), Integer.toString(functionaddress[i]));
                            }
                        }
                    }
                }
                myWriter.write(data + "\n");
            }


            myWriter.close();
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


    public static void read_assembly(String Program){
        try {
            File myObj = new File("Programs/Intermediates/Assembly/" + Program + "_Assembly.txt");
            FileWriter myWriter = new FileWriter("Programs/MachineCode/" + Program + ".mem");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                //System.out.println(data);¨
                int penis = find_name(data);

                /* 

                System.out.println("penis: " + penis);


                //System.out.println(penis);
                System.out.println("String:" + Integer.toString(penis));
                System.out.println("ex: " + String.format("0x%08X", penis));
                System.out.println("Hex:" + Integer.toHexString(penis));
            
                */
                
                //myWriter.write(Integer.toHexString(find_name(data)) + "\n");
                myWriter.write(Integer.toHexString(penis) + "\n");
            }
            myWriter.close();
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static int find_name(String instruction){
        int val = 0;
        /*  ARITHMETIC INSTRUCTIONS  */
                /*
                add rd, rs1, rs2
                sub rd, rs1, rs2
                mult rd, rs1, rs2
                multfp rd, rs1, rs2
                and rd, rs1, rs2
                or rd, rs1, rs2
                xor rd, rs1, rs2
                mac rd, rs1, rs2

                lw rd, rs1
                sw rd, rs1

                */

        String AssemblyInst = instruction.substring(0, instruction.indexOf(" "));

        System.out.println(AssemblyInst + "*" + "\n");

        /* 

        switch(AssemblyInst){
            // Type S1

            case "add":
                val = machine_gen(0, 0, 17, instruction);
            case "sub":
                val = machine_gen(0, 1, 17, instruction);
            case "mult":
                val = machine_gen(0, 3, 17, instruction);
            case "sll":
                val = machine_gen(0, 4, 17, instruction);
            case "srl":
                val = machine_gen(0, 5, 17, instruction);
            case "sla": 
                val = machine_gen(0, 6, 17, instruction);
            case "and": 
                val = machine_gen(0, 7, 17, instruction);
            case "or": 
                val = machine_gen(0, 8, 17, instruction);
            case "xor":
                val = machine_gen(0, 9, 17, instruction);
            case "fpmul":
                val = machine_gen(0, 10, 17, instruction);
            case "mac":
                val = machine_gen(0, 11, 17, instruction);
            case "lw":
                val = machine_gen(0, 12, 17, instruction);
            case "sw":
                val = machine_gen(0, 13, 17, instruction);

            // Type S2    

            case "addi":
                val = machine_gen(1, 0, 17, instruction);
            case "li":
                val = machine_gen(1, 1, 17, instruction);
            case "liu": 
                val = machine_gen(1, 2, 17, instruction);

            // Type S3 

            case "lwi": 
                val = machine_gen(2, 0, 20, instruction);
            case "swi": 
                val = machine_gen(2, 1, 20, instruction);


            // Type S4

            case "beq": 
                val = machine_gen(3, 0, 19, instruction);
            case "bne": 
                val = machine_gen(3, 1, 19, instruction); 
            case "bge": 
                val = machine_gen(3, 2, 19, instruction); 
            case "blt": 
                val = machine_gen(3, 3, 19, instruction); 
            

            // Type V1 

            case "vadd":
                val = machine_gen(4, 0, 17, instruction);
            case "vsub":
                val = machine_gen(4, 1, 17, instruction);
            case "vmult":
                val = machine_gen(4, 3, 17, instruction);
            case "vsll":
                val = machine_gen(4, 4, 17, instruction);
            case "vsrl":
                val = machine_gen(4, 5, 17, instruction);
            case "vsla": 
                val = machine_gen(4, 6, 17, instruction);
            case "vand": 
                val = machine_gen(4, 7, 17, instruction);
            case "vor": 
                val = machine_gen(4, 8, 17, instruction);
            case "vxor":
                val = machine_gen(4, 9, 17, instruction);
            case "vfpmul":
                val = machine_gen(4, 10, 17, instruction);

            // Type V2 

            case "vaddi":
                val = machine_gen(5, 0, 17, instruction);
            case "vsubi":
                val = machine_gen(5, 1, 17, instruction);
            case "vmulti":
                val = machine_gen(5, 3, 17, instruction);
            case "vslli":
                val = machine_gen(5, 4, 17, instruction);
            case "vsrli":
                val = machine_gen(5, 5, 17, instruction);
            case "vslai": 
                val = machine_gen(5, 6, 17, instruction);
            case "vandi": 
                val = machine_gen(5, 7, 17, instruction);
            case "vori": 
                val = machine_gen(5, 8, 17, instruction);
            case "vxori":
                val = machine_gen(5, 9, 17, instruction);
            case "vfpmuli":
                val = machine_gen(5, 10, 17, instruction);

            // Type V3 

            case "vlwi": 
                val = machine_gen(6, 0, 20, instruction);
            case "vswi": 
                val = machine_gen(6, 1, 20, instruction);

            // Type V4 

            case "vsadd":
                val = machine_gen(7, 0, 17, instruction);
            case "vssub":
                val = machine_gen(7, 1, 17, instruction);
            case "vsmult":
                val = machine_gen(7, 3, 17, instruction);
            case "vssll":
                val = machine_gen(7, 4, 17, instruction);
            case "vssrl":
                val = machine_gen(7, 5, 17, instruction);
            case "vssla": 
                val = machine_gen(7, 6, 17, instruction);
            case "vsand": 
                val = machine_gen(7, 7, 17, instruction);
            case "vsor": 
                val = machine_gen(7, 8, 17, instruction);
            case "vsxor":
                val = machine_gen(7, 9, 17, instruction);
            case "vsfpmul":
                val = machine_gen(7, 10, 17, instruction);

        }

        */

        switch(AssemblyInst){
            case "add":
                val = machine_gen(0, 0, 17, instruction);
                break;
            case "sub":
                val = machine_gen(0, 1, 17, instruction);
                break;
            case "mult":
                val = machine_gen(0, 3, 17, instruction);
                break;
            case "sll":
                val = machine_gen(0, 4, 17, instruction);
                break;
            case "srl":
                val = machine_gen(0, 5, 17, instruction);
                break;
            case "sla": 
                val = machine_gen(0, 6, 17, instruction);
                break;
            case "and": 
                val = machine_gen(0, 7, 17, instruction);
                break;
            case "or": 
                val = machine_gen(0, 8, 17, instruction);
                break;
            case "xor":
                val = machine_gen(0, 9, 17, instruction);
                break;
            case "fpmul":
                val = machine_gen(0, 10, 17, instruction);
                break;
            case "mac":
                val = machine_gen(0, 11, 17, instruction);
                break;
            case "lw":
                val = machine_gen(0, 12, 17, instruction);
                break;
            case "sw":
                val = machine_gen(0, 13, 17, instruction);
                break;
            case "addi":
                val = machine_gen(1, 0, 17, instruction);
                break;
            case "li":
                val = machine_gen(1, 1, 17, instruction);
                System.out.println("penis");
                break;
            case "liu": 
                val = machine_gen(1, 2, 17, instruction);
                break;
            case "lwi": 
                val = machine_gen(2, 0, 20, instruction);
                break;
            case "swi": 
                val = machine_gen(2, 1, 20, instruction);
                break;
            case "beq": 
                val = machine_gen(3, 0, 19, instruction);
                break;
            case "bne": 
                val = machine_gen(3, 1, 19, instruction); 
                break;
            case "bge": 
                val = machine_gen(3, 2, 19, instruction); 
                break;
            case "blt": 
                val = machine_gen(3, 3, 19, instruction); 
                break;
            case "vadd":
                val = machine_gen(4, 0, 17, instruction);
                break;
            case "vsub":
                val = machine_gen(4, 1, 17, instruction);
                break;
            case "vmult":
                val = machine_gen(4, 3, 17, instruction);
                break;
            case "vsll":
                val = machine_gen(4, 4, 17, instruction);
                break;
            case "vsrl":
                val = machine_gen(4, 5, 17, instruction);
                break;
            case "vsla": 
                val = machine_gen(4, 6, 17, instruction);
                break;
            case "vand": 
                val = machine_gen(4, 7, 17, instruction);
                break;
            case "vor": 
                val = machine_gen(4, 8, 17, instruction);
                break;
            case "vxor":
                val = machine_gen(4, 9, 17, instruction);
                break;
            case "vfpmul":
                val = machine_gen(4, 10, 17, instruction);
                break;
            case "vaddi":
                val = machine_gen(5, 0, 17, instruction);
                break;
            case "vsubi":
                val = machine_gen(5, 1, 17, instruction);
                break;
            case "vli":
                val = machine_gen(5, 2, 17, instruction);
                break;
            case "vlui":
                val = machine_gen(5, 3, 17, instruction);
                break;    
            case "vmulti":
                val = machine_gen(5, 4, 17, instruction);
                break;
            case "vslli":
                val = machine_gen(5, 5, 17, instruction);
                break;
            case "vsrli":
                val = machine_gen(5, 6, 17, instruction);
                break;
            case "vslai": 
                val = machine_gen(5, 7, 17, instruction);
                break;
            case "vandi": 
                val = machine_gen(5, 8, 17, instruction);
                break;
            case "vori": 
                val = machine_gen(5, 9, 17, instruction);
                break;
            case "vxori":
                val = machine_gen(5, 10, 17, instruction);
                break;
            case "vfpmuli":
                val = machine_gen(5, 11, 17, instruction);
                break;
            case "vlwi": 
                val = machine_gen(6, 0, 20, instruction);
                break;
            case "vswi": 
                val = machine_gen(6, 1, 20, instruction);
                break;
            case "vsadd":
                val = machine_gen(7, 0, 17, instruction);
                break;
            case "vssub":
                val = machine_gen(7, 1, 17, instruction);
                break;
            case "vsmult":
                val = machine_gen(7, 3, 17, instruction);
                break;
            case "vssll":
                val = machine_gen(7, 4, 17, instruction);
                break;
            case "vssrl":
                val = machine_gen(7, 5, 17, instruction);
                break;
            case "vssla": 
                val = machine_gen(7, 6, 17, instruction);
                break;
            case "vsand": 
                val = machine_gen(7, 7, 17, instruction);
                break;
            case "vsor": 
                val = machine_gen(7, 8, 17, instruction);
                break;
            case "vsxor":
                val = machine_gen(7, 9, 17, instruction);
                break;
            case "vsfpmul":
                val = machine_gen(7, 10, 17, instruction);
                break;

        }

        return val;
    }

    public static int machine_gen(int type, int op, int opshift, String instruction){
        //System.out.println(instruction);

        int val = 0; 
        val = 0b000000000000000000000000 | (type << 21);
        val = val | (op << opshift);

        System.out.println(type);

        val = (val | find_arguments(instruction,type));

    

        return val; 
    }


    public static int find_arguments(String instruction, int type){
        int val=0;

        switch(type){
            case 0:
                int rd_index = instruction.indexOf("x");
                int rd = find_register(instruction.substring(rd_index, rd_index + 3));

                int rs1_index = instruction.indexOf("x", rd_index + 1);

                int rs1 = find_register(instruction.substring(rs1_index, rs1_index + 3));

                int rs2_index = instruction.indexOf("x", rs1_index + 1);
                int rs2 = find_register(instruction.substring(rs2_index));

                /*

                System.out.println(rd_index);
                System.out.println(rd);
                System.out.println(rs1_index);
                System.out.println(rs1);
                System.out.println(rs2_index);
                System.out.println(rs2);

                */
                

                return (rd << 12) + (rs1 << 7) + (rs2 << 2);
            case 1:
                rd_index = instruction.indexOf("x");
                rd = find_register(instruction.substring(rd_index, rd_index + 3).replace(" ", ""));

                int imm = find_register(instruction.substring(rd_index + 3));

                if(instruction.contains("addi") && instruction.contains("-")){
                    imm = -imm;
                }

                return (rd << 12) + (imm & 0b111111111111);
            case 2:
                rd_index = instruction.indexOf("x");
                rd = find_register(instruction.substring(rd_index, rd_index + 3).replace(" ", ""));

                int imm2 = find_register(instruction.substring(rd_index + 3));

                //System.out.println(imm2);

                return (rd << 17) + (imm2 & 0b1111111111111111);

            case 3:
                System.out.println("case 3");

                rs1_index = instruction.indexOf("x");
                rs1 = find_register(instruction.substring(rs1_index, rs1_index + 3).replace(" ", ""));

                rs2_index = instruction.indexOf("x", rs1_index + 1);
                rs2 = find_register(instruction.substring(rs2_index, rs2_index + 3).replace(" ", ""));

                int offset = find_register(instruction.substring(rs2_index + 3).replace(" ", ""));

                if(instruction.substring(rs2_index + 3).contains("-")){
                    offset = -offset;
                }

                //System.out.println(rs2);
                return (rs1 << 14) + (rs2 << 9) + (offset & 0b111111111);
            case 4:
                rd_index = instruction.indexOf("x");
                rd = find_register(instruction.substring(rd_index, rd_index + 3));

                rs1_index = instruction.indexOf("x", rd_index + 1);

                rs1 = find_register(instruction.substring(rs1_index, rs1_index + 3));

                rs2_index = instruction.indexOf("x", rs1_index + 1);
                rs2 = find_register(instruction.substring(rs2_index));

                /*

                System.out.println(rd_index);
                System.out.println(rd);
                System.out.println(rs1_index);
                System.out.println(rs1);
                System.out.println(rs2_index);
                System.out.println(rs2);

                */
                    

                return (rd << 14) + (rs1 << 11) + (rs2 << 8);
            case 5:
                rd_index = instruction.indexOf("x");
                rd = find_register(instruction.substring(rd_index, rd_index + 3).replace(" ", ""));

                imm = find_register(instruction.substring(rd_index + 3));

                if(instruction.contains("addi") && instruction.contains("-")){
                    imm = -imm;
                }

                return (rd << 12) + (imm & 0b111111111111);
            case 6:
                rd_index = instruction.indexOf("x");
                rd = find_register(instruction.substring(rd_index, rd_index + 3).replace(" ", ""));

                imm2 = find_register(instruction.substring(rd_index + 3));

                //System.out.println(imm2);

                return (rd << 17) + ((imm2 & 0b1111111111111111) << 1);

            case 7:
                rd_index = instruction.indexOf("x");
                rd = find_register(instruction.substring(rd_index, rd_index + 3));

                rs1_index = instruction.indexOf("x", rd_index + 1);

                rs1 = find_register(instruction.substring(rs1_index, rs1_index + 3));

                rs2_index = instruction.indexOf("x", rs1_index + 1);
                rs2 = find_register(instruction.substring(rs2_index));

                /*

                System.out.println(rd_index);
                System.out.println(rd);
                System.out.println(rs1_index);
                System.out.println(rs1);
                System.out.println(rs2_index);
                System.out.println(rs2);

                */
                    

                return (rd << 13) + (rs1 << 9) + (rs2 << 4);
        }

        return val;
    }

    public static int find_register(String instruction){
        String register = instruction.replaceAll(("[^0-9]"), "");
        return Integer.parseInt(register);
    }
}
