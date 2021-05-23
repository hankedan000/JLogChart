# JLogChart
A simple plotting component that I made while developing my racing data acquisition project. The UI is inspired by [MegaLogViewer](https://www.efianalytics.com/MegaLogViewer/).

# Example
![JLogChartDemo](docs/imgs/JLogChartDemo.png?raw=true "JLogChartDemo")

```java
public static void main(String[] args) {
    FlatDarkLaf.install();
    
    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
            JFrame frame = new JFrame("JLogChart Standalone");
            frame.setLayout(new BorderLayout());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Create and show the JLogChart GUI
            JLogChart jlc = new JLogChart();
            jlc.setPreferredSize(new Dimension(600, 400));
            jlc.setSingleY_Scale(true);
            jlc.setSampleRate(100.0);
            frame.add(jlc, BorderLayout.CENTER);
            
            int N_CYCLES = 3;
            int N_SAMPS = 1000;
            double radPerSamp = Math.PI * 2 * N_CYCLES / N_SAMPS;
            List<Double> sinData = new ArrayList();
            List<Double> cosData = new ArrayList();
            List<Double> negSinData = new ArrayList();
            for (int i=0; i<N_SAMPS; i++) {
                sinData.add(Math.sin(i * radPerSamp));
                cosData.add(Math.cos(i * radPerSamp) * 2.0);
                negSinData.add(Math.sin(i * radPerSamp) - 2.0);
            }
            jlc.addSeries("sin", sinData);
            jlc.addSeries("cos", cosData);
            jlc.addSeries("sin - 2", negSinData);
            
            // Show the GUI
            frame.pack();
            frame.setVisible(true);
        }
    });
}
```