import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.regex.Pattern;

public class RevAndLowercase
{
    public static void main(String [] args) throws Exception
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            cl.getResourceAsStream("german.sorted"), "ISO8859-1"));

        Writer w = new OutputStreamWriter(new
            BufferedOutputStream(new FileOutputStream("inverted.txt")), "UTF-8");
        String line;
        Pattern p = Pattern.compile("[\\t]");
        while ((line = reader.readLine()) != null)
        {
            String [] chunks = p.split(line.trim());
            chunks[0] = new StringBuilder(chunks[0].toLowerCase()).reverse().toString();
            chunks[1] = chunks[1].toLowerCase();

            w.write(chunks[0] + "\t" + chunks[1] + "\t" + chunks[2]);
            w.write("\n");
        }
        
        w.close();
        reader.close();
    }
}
