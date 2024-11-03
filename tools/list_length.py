import pandas as pd

# Load the CSV file
FILE_PATH = "../app/src/main/assets/quotes.csv"
df = pd.read_csv(FILE_PATH, delimiter="|")

# Filter entries based on character count
short_entries = df[df["quote"].str.len() < 200].sort_values(
    by="quote", key=lambda x: x.str.len()
)
long_entries = df[df["quote"].str.len() > 400].sort_values(
    by="quote", key=lambda x: -x.str.len()
)

# Print only the quotes without any other columns
print("Entries with fewer than 200 characters (sorted by length in ascending order):")
for quote in short_entries["quote"]:
    print(quote)

print("\nEntries with more than 400 characters (sorted by length in descending order):")
for quote in long_entries["quote"]:
    print(quote)
