# EdgeInfer

This is a repo for Android application of Powerinfer.

## Setup and Installation

### Prerequisites

- Android Studio 2024.2.1 or later

### Installation

1. Clone the repo to your local machine using the following command:

```bash
git clone https://github.com/Si1w/EdgeInfer.git
```

2. Open the project in Android Studio.

3. Build and run the project in Android Studio.

OR 

1. Download the APK file from the [release](https://github.com/Si1w/EdgeInfer/releases) page.

### ADD YOUR OWN MODEL

1. Open the file [`./android/app/src/main/java/com/example/edgeinfer/MainActivity.java`](https://github.com/Si1w/EdgeInfer/blob/main/android/app/src/main/java/com/example/androidpowerinfer/MainActivity.kt).

2. Find the following code:

```kotlin
val models = listOf(
      Downloadable(
            name = "Bamboo 7B (Q4)",
            Uri.parse(
                  "https://huggingface.co/PowerInfer/Bamboo-base-v0.1-gguf/" +
                  "resolve/main/bamboo-7b-v0.1.Q4_0.powerinfer.gguf?download=true"
            ),
            File(getExternalFilesDir(null), "bamboo-7b-v0.1.Q4_0.powerinfer.gguf")
      ),
      Downloadable(
            name = "Bamboo DPO (Q4)",
            Uri.parse(
                  "https://huggingface.co/PowerInfer/Bamboo-DPO-v0.1-gguf/" +
                  "resolve/main/bamboo-7b-dpo-v0.1.Q4_0.powerinfer.gguf?download=true"
            ),
            File(getExternalFilesDir(null), "bamboo-7b-dpo-v0.1.Q4_0.powerinfer.gguf")
      )
)
```

3. Add your own model by adding a new `Downloadable` object to the `models` list.

## Paper and Citation
More technical details can be found in [paper](https://ipads.se.sjtu.edu.cn/_media/publications/powerinfer-20231219.pdf).

```bibtex
@misc{song2023powerinfer,
      title={PowerInfer: Fast Large Language Model Serving with a Consumer-grade GPU},
      author={Yixin Song and Zeyu Mi and Haotong Xie and Haibo Chen},
      year={2023},
      eprint={2312.12456},
      archivePrefix={arXiv},
      primaryClass={cs.LG}
}
```
