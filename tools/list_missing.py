import pandas as pd

FILE_PATH = "../app/src/main/assets/quotes.csv"
df = pd.read_csv(FILE_PATH, delimiter="|")

all_times = [f"{hour:02d}:{minute:02d}" for hour in range(24) for minute in range(60)]

existing_times = set(df["hour"].unique())

missing_times = [time for time in all_times if time not in existing_times]

print("Times with no entries:")
print(missing_times)
