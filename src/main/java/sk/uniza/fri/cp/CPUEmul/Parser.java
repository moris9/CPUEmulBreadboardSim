package sk.uniza.fri.cp.CPUEmul;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import javafx.concurrent.Task;
import sk.uniza.fri.cp.CPUEmul.Exceptions.InvalidCodeLinesException;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsuje prikazy z editoru do pola pre vykonavanie
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Parser extends Task<Program>{

    private static final String LABEL_PATTERN = "^\\s*\\w+:"; //po zaciatku riadku ziadna alebo viac whitespace znakov, [a-zA-Z_0-9] a dvojbodka
    private static final String COMMENT_PATTERN = "(?m);.+$"; //vsetko za bodkociarkou po koniec riadku

    //KOD
    private String code;
    private boolean hasError; //je v kode chyba?
    private int numOfCodeLines;

    //PROGRAM
    private ArrayList<Instruction> instructions;
    private TreeMap<Integer, Integer> lineIndexToInstructionIndex; //prevodnik medzi riadkom a instrukciou
    private ArrayList<Byte> progMemory;
    private TreeMap<Integer, Integer> interruptionLabels; //<cislo prerusenia, adresa instrukcie>


    //POMOCNE
    //TreeMap pre zapisanie navestia a indexu instrukcie na ktoru odkazuje
    private TreeMap<String, Integer> labels = new TreeMap<>();
    //pole pre uchovanie indexov instrukcii, ktore obsahuju navestie pre jeho nahradenie indexom skoku
    private ArrayList<Integer> instructionsWithLabels;

    //CHYBY
    //uchovavanie chyb pre vypis na konzolu
    private ArrayList<String> errors;
    private ArrayList<Integer> errorLines;
    //private PrintWriter printWriter;


	public Parser(String code, int numberOfCodeLines){
        this.code = code;
        numOfCodeLines = numberOfCodeLines;
        //printWriter = pw;
        instructions = new ArrayList<>(numberOfCodeLines);
        instructionsWithLabels = new ArrayList<>();
        progMemory = new ArrayList<>();
        lineIndexToInstructionIndex = new TreeMap<>();
        interruptionLabels = new TreeMap<>();

        hasError = false;
	}

    /**
     * Parsovanie kodu programu
     */

    @Override
    protected Program call() throws InvalidCodeLinesException {
        int lineIndex = 0;

        //Priprava na parsovanie instrukcie
        List<String> instructionParts = new ArrayList<>(3);
        Pattern instructionPattern = Pattern.compile("[^\\s\"',]+|'.'");
        Matcher instructionMatcher;

        //odstranenie vsetkych komentarov
        Pattern pat = Pattern.compile(COMMENT_PATTERN);
        code = pat.matcher(code).replaceAll("");

        //pattern pre navestia
        Pattern patternLabel = Pattern.compile(LABEL_PATTERN);

        //citanie po riadkoch kodu
        try(BufferedReader br = new BufferedReader(new StringReader(code))){
            String line = br.readLine();

            //citaj pokial nebolo vlakno ukoncene alebo nie je koniec kodu
            while (!isCancelled() && line != null){

                //nastavenie progresu
                updateProgress(lineIndex+1, numOfCodeLines);

                //kontrola ci riadok obsahuje navestie
                Matcher matcherLabel = patternLabel.matcher(line);
                if(matcherLabel.find()){
                    String label = matcherLabel.group();
                    label = label.substring(0, label.length()-1).trim(); //odstranenie dvojbodky a medzier

                    //existuje uz dane navestie?
                    if(!labels.containsKey(label)) {
                        //kontrola, ci sa jedna o navestie pre prerusenie
                        if(label.matches("int.*")){
                            //navestie zacina int -> musi byt int00 az int0f
                            if(label.matches("int0\\p{XDigit}")){
                                //splna podmienky navestia pre prerusenie
                                int intNumber = Integer.parseInt(label.substring(4,5), 16);

                                if(!interruptionLabels.containsKey(intNumber)){
                                    interruptionLabels.put(intNumber, instructions.size()); //index nasledujucej instrukcie, ktora bude pridana
                                } else{
                                    addError("Návestie pre prerušenie " + label + " už existuje", lineIndex);
                                }

                            } else {
                                //asi to malo byt prerusenie ale je v zlom tvare
                                addError("Nesprávny tvar návestia pre prerušenie '" + label + "'", lineIndex);
                            }
                        } else {
                            //obycajne navestie
                            labels.put(label, instructions.size()); //index nasledujucej instrukcie, ktora bude pridana
                        }
                    } else {
                        addError("Návestie " + label + " už existuje", lineIndex);
                    }

                    //odstran navestie z riadku
                    line = matcherLabel.replaceFirst("");
                }

                //idealne by v riadku mala ostat uz iba instrukcia
                line = line.trim();
                //ak je riadok prazdny, chod na dalsi
                if(line.isEmpty()){
                    lineIndex++;
                    line = br.readLine();
                    continue;
                }

                //inak sa pokus rozdelit instrukciu a parametre
                //String[] instructionParts = line.split("\\s*,\\s*|\\s+"); //rozdelenie instrukcie (medzery a ciarka)
                instructionParts.clear();
                instructionMatcher = instructionPattern.matcher(line);
                while (instructionMatcher.find()){
                    instructionParts.add(instructionMatcher.group());
                }

                //je instrukcia platna?
                try {
                    enumInstructionsSet codeInstruction = enumInstructionsSet.valueOf(instructionParts.get(0).toUpperCase());

                    //ak je platna
                    //kontrola poctu parametrov
                    if(codeInstruction.getNumOfParameters() == instructionParts.size()-1) { //porovnanie pozadovanych parametrov s poctom nacitanych (-1 lebo instrukcia samotna)
                        boolean parametersError = false;

                        //kontrola spravnosti parametrov
                        if(codeInstruction.getNumOfParameters() > 0 && !instructionParts.get(1).matches(codeInstruction.getFirstRegex())){ //kontrola prveho parametra
                            //ak nezodpoveda predpisu
                            addError("Nesprávny prvý parameter inštrukcie '" + instructionParts.get(0) + "'", lineIndex);
                            parametersError = true;
                        }

                        if(codeInstruction.getNumOfParameters() > 1 && !instructionParts.get(2).matches(codeInstruction.getSecondRegex())){ //kontrola druheho parametra
                            //ak nezodpoveda predpisu
                            addError("Nesprávny druhý parameter inštrukcie '" + instructionParts.get(0) + "'", lineIndex);
                            parametersError = true;
                        }

                        //instrukcia MVX je specialna
                        if(codeInstruction == enumInstructionsSet.MVX){
                            //ak je prvy operand register C a druhy A - chyba
                            if(instructionParts.get(1).matches("(?i)C") && instructionParts.get(2).matches("(?i)A")){
                                addError("Nesprávny druhý parameter inštrukcie '" + instructionParts.get(0) + "', povolené sú iba reg. S,M", lineIndex);
                                parametersError = true;
                            } else if (instructionParts.get(1).matches("(?i)S|M") && instructionParts.get(2).matches("(?i)S|M")){
                                addError("Nesprávny druhý parameter inštrukcie '" + instructionParts.get(0) + "', povolený je iba reg. A", lineIndex);
                                parametersError = true;
                            }
                        }

                        //ak je cela instrukcia aj s parametrami v poriadku, mozeme ju zaviest do programu (ak je aj chybny, kontroluje sa este spravnost navesti v instrukciach)
                        if(!parametersError){
                            switch (codeInstruction.getNumOfParameters()){
                                case 0:
                                    instructions.add(new Instruction(codeInstruction));
                                    break;
                                case 1:
                                    //ak je instrukcia BYTE, zaved konstantu do pamate ale nie instrukciu do programu
                                    if(codeInstruction == enumInstructionsSet.BYTE){
                                        byte b = (byte) parseConstant(instructionParts.get(1));
                                        progMemory.add(b);
                                    } else {
                                        instructions.add(new Instruction(codeInstruction, instructionParts.get(1)));
                                    }
                                    break;
                                case 2:
                                    instructions.add(new Instruction(codeInstruction, instructionParts.get(1), instructionParts.get(2)));
                                    break;
                            }

                            //ak instrukcia pouziva navestie
                            if(codeInstruction.usesLabel()){
                                //pridanie indexu inst. pre jej zmenu (navestie -> index)
                                instructionsWithLabels.add(instructions.size()-1);
                            }

                            //zaradenie do pervodnika medzi riadkami kodu a indexami instrukcii (pre breaky)
                            lineIndexToInstructionIndex.put(lineIndex, instructions.size()-1);
                        }

                    } else { //pocet parametrov neodpoveda
                        addError("Zlý počet parametrov inštrukcie '" + instructionParts.get(0) + "', požadovaný počet '" + codeInstruction.getNumOfParameters() + "'", lineIndex);
                    }

                } catch (IllegalArgumentException e){
                    //ak instrukcia nie je platna (nie je v zozname instrukcii)
                    addError("Neplatná inštrukcia '" + instructionParts.get(0) + "'", lineIndex);
                }

                lineIndex++;
                line = br.readLine();
            }

            updateProgress(numOfCodeLines, numOfCodeLines);
        } catch (IOException e) {
            //chyba so streamom stringu z kodu
            e.printStackTrace();
        }

        //ak je program v poriadku, preved navestia na indexy
        if(!hasError && labels.size() > 0) transformLabelsToIndexes();

        //zmensenie pola arraylistu na minimum
        instructions.trimToSize();

        //zostavenie programu
        if(!hasError){
            return new Program(instructions, progMemory, lineIndexToInstructionIndex, interruptionLabels);
        } else {
            //vyhodit vynimku s chybnymi riadkami
            int[] lines = errorLines.stream().filter(Objects::nonNull).mapToInt(i -> i).toArray();
            String[] errorsMsgs = errors.toArray(new String[0]);
            throw new InvalidCodeLinesException(lines, errorsMsgs);
        }
    }

    /**
     * Metoda prevadza navestia na indexy, kam navestie ukazuje
     */
    private void transformLabelsToIndexes(){
	    //prechadzaj vsetky instrukcie, ktore pouzivaju navestie
        for (Integer indexOfInstruction: instructionsWithLabels) {
            //zober instrukciu s navestim
            Instruction inst = instructions.get(indexOfInstruction);
            //zober jej navestie
            String label = inst.getFirstParameter();

            if(label != null){
                //pokus sa najst odpovedajuci index instrukcie, na ktore navestie ukazuje
                Integer jumpIndex = labels.get(label);
                //ak sa navestie naslo
                if(jumpIndex != null){
                    //zmen navestie instrukcie na index skoku
                    inst.setFirstParameter(Integer.toString(jumpIndex));
                } else {
                    //ak sa navestie nenaslo
                    addError("Neplatné návestie '" + label + "'", getLineOfInstruction(indexOfInstruction));
                }
            } else {
                addError("Chyba v inštrukcií", getLineOfInstruction(indexOfInstruction));
            }
        }
    }

    /**
     * hlada a vracia riadok instrukcie
     * @param instructionIndex Index instrukcie ku ktorej sa hlada riaok
     * @return Index riadku na ktorom je instrukcia, ak sa nenasla, vracia -1
     */
    private int getLineOfInstruction(int instructionIndex){
        for (Map.Entry<Integer, Integer> entry : lineIndexToInstructionIndex.entrySet()) {
            if (Objects.equals(instructionIndex, entry.getValue())) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private void addError(String err, int lineIndex){
        if(!hasError){
            errors = new ArrayList<>();
            errorLines = new ArrayList<>();
        }

        errors.add("["+(lineIndex+1)+"] " + err);
        errorLines.add(lineIndex);
        hasError = true;
    }


    //STATICKE

    /**
     * Funkcia prijima retazec a kontroluje, ci obsahuje instrukciu, ktora nemusi byt spravna
     * @param line Retazec s instrukciou
     * @return True - riadok obsahuje instrukciu
     */
    public static boolean isInstrucionLine(String line){
        //odstranenie komentarov
        Pattern patternComment = Pattern.compile(COMMENT_PATTERN);
        line = patternComment.matcher(line).replaceAll("");
        if(line.isEmpty()) return false;

        //odstranenie navestia
        Pattern patternLabel = Pattern.compile(LABEL_PATTERN);
        line = patternLabel.matcher(line).replaceAll("");
        if(line.isEmpty()) return false;

        line = line.trim();

        //pokus sa rozdelit instrukciu a parametre
        String[] instructionParts = line.split("\\s*,\\s*|\\s+"); //rozdelenie instrukcie (medzery a ciarka)

        //je instrukcia platna?
        try {
            enumInstructionsSet codeInstruction = enumInstructionsSet.valueOf(instructionParts[0].toUpperCase());
            return true;
        } catch (IllegalArgumentException e){
            return false;
        }
    }

    public static int parseConstant(String constant){

        //hexa
        if(constant.matches("0x.+")){
            return Integer.parseInt(constant.substring(2), 16);
        }

        //binary
        if(constant.matches("[0-1]+b$")){
            return Integer.parseInt(constant.substring(0, constant.length()-1), 2);
        }

        //octa
        if(constant.matches("^0[0-7]+")){
            return Integer.parseInt(constant, 8);
        }

        //dec
        if(constant.matches("(^[1-9][0-9]*)|(^0$)")){
            return Integer.parseInt(constant);
        }

        //char
        if(constant.matches(iRegexes.rByteChar)){
            return (int) constant.charAt(1);
        }

        try{
            return Integer.parseInt(constant);
        } catch (NumberFormatException e){
            return -1;
        }
    }

}//end Parser
