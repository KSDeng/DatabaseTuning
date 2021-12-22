
import java.io.File;
import java.util.Scanner;
import java.io.FileNotFoundException;

public class mainDriver {

	public static void main(String[] args) throws FileNotFoundException {
		
		if (args.length < 1) {
			System.out.println("param1: the path of input file");
			System.exit(1);
		}

		File file = new File(args[0]);
		Scanner reader = new Scanner(file);

		while (reader.hasNextLine()) {
			String str = reader.nextLine();
			if (str.length() == 0) {
				continue;
			}
			String[] values = str.split(",");

			switch (values[0].charAt(0)) {
				case 'N': {
					System.out.println("NewOrder Xact");
					break;
				}
				case 'P': {
					System.out.println("Payment Xact");
					break;
				}
				case 'D': {
					System.out.println("Delivery Xact");
					break;

				}
				case 'O': {
					System.out.println("OrderStatus Xact");
					break;
				}
				case 'S': {
					System.out.println("StockLevel Xact");
					break;
				}
				case 'I': {
					System.out.println("PopularItem Xact");
					break;
				}
				case 'T': {
					System.out.println("TopBalance Xact");
					break;
				}
				case 'R': {
					System.out.println("RelatedCustomer Xact");
					break;
				}
				default: {
				}

			}
		}


	}

}
