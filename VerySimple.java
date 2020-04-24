// prints out the literal 3 to the terminal, using 'super' so that dynamic
// dispatch is not necessary
class Main extends Lib {
    public void main() {
		printMessage();
    }

    public void printMessage(){
        super.printInt(4);
    }
}
