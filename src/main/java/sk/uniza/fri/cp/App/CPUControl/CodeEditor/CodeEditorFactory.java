package sk.uniza.fri.cp.App.CPUControl.CodeEditor;

import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Faktorka na vytvorenie editora kódu.
 * Definuje kľúčové slová, ktoré sa zvýrazňujú ako aj nastavenia paragrafov.
 *
 * @author Tomáš Hianik
 * @created 19.2.2017.
 */
public class CodeEditorFactory {
    /**
     * Specifikacie pre zvyraznovanie slov v editore
     */
    private static final String[] KEYWORDS_ARITHMETIC_AND_LOGIC = new String[] {
            "ADD","ADC","ADI","SUB","SUC","SBI","AND","ANI","ORR","ORI","XOR","XRI","INC","INX","DEC","DCX","CMP","CMI"	};
    private static final String[] KEYWORDS_SHIFT_AND_ROTATE = new String[] {
            "SHL","SHR","SCR","RTL","RCL","RTR","RCR" };
    private static final String[] KEYWORDS_MOVE = new String[] {
            "MOV","MVI","MXI","MVX","MMR","LMI","LMR","SMI","SMR","INN","OUT","PUS","POP","STR","LDR" };
    private static final String[] KEYWORDS_BRANCH = new String[] {
            "JMP","JZR","JNZ","JCY","JNC","CAL","CZR","CNZ","CCY","CNC","RET" };
    private static final String[] KEYWORDS_SPECIAL = new String[] {
            "EIT","DIT","SCALL","BYTE" };

    private static final String KEYWORDS_ARITHMETIC_AND_LOGIC_PATTERN = "\\b(" + String.join("|", KEYWORDS_ARITHMETIC_AND_LOGIC) + ")\\b";
    private static final String KEYWORDS_SHIFT_AND_ROTATE_PATTERN = "\\b(" + String.join("|", KEYWORDS_SHIFT_AND_ROTATE) + ")\\b";
    private static final String KEYWORDS_MOVE_PATTERN = "\\b(" + String.join("|", KEYWORDS_MOVE) + ")\\b";
    private static final String KEYWORDS_BRANCH_PATTERN = "\\b(" + String.join("|", KEYWORDS_BRANCH) + ")\\b";
    private static final String KEYWORDS_SPECIAL_PATTERN = "\\b(" + String.join("|", KEYWORDS_SPECIAL) + ")\\b";
    private static final String LABEL_PATTERN = "[a-zA-Z0-9]+:";
    private static final String COMMENT_PATTERN = ";.+[^\n]";

    private static final Pattern PATTERN = Pattern.compile(
            "(?i)(?<KEYWORDSARITHMETICANDLOGIC>" + KEYWORDS_ARITHMETIC_AND_LOGIC_PATTERN + ")"
                    +"|(?<KEYWORDSSHIFTANDROTATE>" + KEYWORDS_SHIFT_AND_ROTATE_PATTERN + ")"
                    +"|(?<KEYWORDSMOVE>" + KEYWORDS_MOVE_PATTERN + ")"
                    +"|(?<KEYWORDSBRANCH>" + KEYWORDS_BRANCH_PATTERN + ")"
                    +"|(?<KEYWORDSSPECIAL>" + KEYWORDS_SPECIAL_PATTERN + ")"
                    + "|(?<LABEL>" + LABEL_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    private CodeEditorFactory() {
    }

    /**
     * Vytvorí a vráti editor kódu podporujúci zvýrazňovanie kľúčových slov,
     * číslovanie riadkov a zaznamenávanie breakpointov.
     *
     * @param parentPane                Panel, do ktorého sa editor vloží.
     * @param observableBreakpointLines Zoznam s breakpointami
     * @return Nová inštancia editora kódu
     */
    public static CodeArea getCodeEditor(Pane parentPane, ObservableSet<Integer> observableBreakpointLines){
        CodeArea codeEditor = new CodeArea();
        parentPane.getChildren().add(new VirtualizedScrollPane<>(codeEditor));
        IntFunction<Node> breakpointFactory = new BreakpointFactory(codeEditor, observableBreakpointLines); //breakpointy
        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(codeEditor); //cisla riadkov
        codeEditor.setParagraphGraphicFactory((int line) -> new HBox(
                breakpointFactory.apply(line),
                lineNumberFactory.apply(line)
        ));    //zobrazenie riadkovania

        codeEditor.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(change -> {
                    codeEditor.setStyleSpans(0, computeHighlighting(codeEditor.getText()));
                });

        return codeEditor;
    }

    /**
     * Farebne zvýraznoňanie v editore kódu
     */
    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORDSARITHMETICANDLOGIC") != null ? "keyword-arithmetic-and-logic" :
                            matcher.group("KEYWORDSSHIFTANDROTATE") != null ? "keyword-shift-and-rotate" :
                                    matcher.group("KEYWORDSMOVE") != null ? "keyword-move" :
                                            matcher.group("KEYWORDSBRANCH") != null ? "keyword-branch" :
                                                    matcher.group("KEYWORDSSPECIAL") != null ? "keyword-special" :
                                                            matcher.group("LABEL") != null ? "label" :
                                                                    matcher.group("COMMENT") != null ? "comment" :
                                                                            null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
