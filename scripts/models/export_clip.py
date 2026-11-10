#!/usr/bin/env python3
"""
Cümleyle arama modellerini LiteRT'e (.tflite, int8 dinamik ağırlık) çevirir. GitHub Actions'ta çalışır
(.github/workflows/models.yml); sonuç "modeller" sürümüne yüklenir, APK derlemesi oradan indirir.

- Fotoğraf: OpenAI CLIP ViT-B/32 görüntü kodlayıcısı (MIT). Girdi [1,224,224,3] RGB 0..1, çıktı [1,512] birim vektör.
- Metin: sentence-transformers/clip-ViT-B-32-multilingual-v1 (Apache-2.0, Türkçe dahil 50+ dil, aynı vektör uzayı).
  Girdi [1,64] int32 belirteç kimlikleri (0 = [PAD], maske model içinde), çıktı [1,512] birim vektör.
"""
import json
import os
import sys

import numpy as np
import torch

OUT = sys.argv[1] if len(sys.argv) > 1 else "out"
SEQ = 64
os.makedirs(OUT, exist_ok=True)
torch.manual_seed(0)

import litert_torch  # noqa: E402
from ai_edge_litert.interpreter import Interpreter  # noqa: E402
from sentence_transformers import SentenceTransformer  # noqa: E402
from transformers import CLIPVisionModelWithProjection  # noqa: E402

MEAN = torch.tensor([0.48145466, 0.4578275, 0.40821073]).view(1, 3, 1, 1)
STD = torch.tensor([0.26862954, 0.26130258, 0.27577711]).view(1, 3, 1, 1)


class ImageEncoder(torch.nn.Module):
    def __init__(self):
        super().__init__()
        self.model = CLIPVisionModelWithProjection.from_pretrained(
            "openai/clip-vit-base-patch32", attn_implementation="eager"
        ).eval()

    def forward(self, x):
        x = (x.permute(0, 3, 1, 2) - MEAN) / STD
        e = self.model(pixel_values=x).image_embeds
        return e / e.norm(dim=-1, keepdim=True)


st = SentenceTransformer("sentence-transformers/clip-ViT-B-32-multilingual-v1", device="cpu")
bert = st[0].auto_model.eval()
try:
    bert.config._attn_implementation = "eager"
except Exception:  # noqa: BLE001
    pass


class TextEncoder(torch.nn.Module):
    def __init__(self):
        super().__init__()
        self.bert = bert
        self.dense = st[2].linear

    def forward(self, ids):
        mask = (ids != 0).long()
        out = self.bert(input_ids=ids.long(), attention_mask=mask).last_hidden_state
        m = mask.unsqueeze(-1).float()
        pooled = (out * m).sum(1) / m.sum(1).clamp(min=1e-9)
        e = self.dense(pooled)
        return e / e.norm(dim=-1, keepdim=True)


def quantize(fp32_path, out_path):
    from ai_edge_quantizer import quantizer, recipe

    qt = quantizer.Quantizer(fp32_path)
    qt.load_quantization_recipe(recipe.dynamic_wi8_afp32())
    result = qt.quantize()
    try:
        result.export_model(out_path, overwrite=True)
    except TypeError:
        result.export_model(out_path)


def convert(module, args, name):
    edge = litert_torch.convert(module.eval(), args)
    fp32 = os.path.join(OUT, f"{name}_fp32.tflite")
    edge.export(fp32)
    final = os.path.join(OUT, f"{name}.tflite")
    quantize(fp32, final)
    os.remove(fp32)
    print(f"{name}: {os.path.getsize(final) / 1e6:.1f} MB")
    return final


def run_tflite(path, x):
    it = Interpreter(model_path=path)
    it.allocate_tensors()
    inp = it.get_input_details()
    print(path, "inputs:", [(d["name"], d["shape"].tolist(), str(d["dtype"])) for d in inp])
    it.set_tensor(inp[0]["index"], x)
    it.invoke()
    return it.get_tensor(it.get_output_details()[0]["index"])


def tokenize(text):
    enc = st.tokenizer(text, padding="max_length", truncation=True, max_length=SEQ)
    return enc["input_ids"]


# ---------------------------------------------------------------- fotoğraf
image = ImageEncoder()
img = torch.rand(1, 224, 224, 3)
image_path = convert(image, (img,), "clip_image")
with torch.no_grad():
    ref = image(img).numpy()
got = run_tflite(image_path, img.numpy().astype(np.float32))
print("image cos:", float((ref * got).sum()))

# ---------------------------------------------------------------- metin
text = TextEncoder()
sentences = [
    "kumsalda gün batımı",
    "Doğum günü pastası ve mumlar",
    "karla kaplı dağlar",
    "a dog playing in the park",
    "İstanbul'da Boğaz köprüsü",
    "çocuklar parkta oynuyor",
]
ids = torch.tensor([tokenize(sentences[0])], dtype=torch.int32)
text_path = convert(text, (ids,), "clip_text")
cases = []
for s in sentences:
    t = torch.tensor([tokenize(s)], dtype=torch.int32)
    with torch.no_grad():
        ref_t = text(t).numpy()
    lib = st.encode([s], normalize_embeddings=True)
    got_t = run_tflite(text_path, t.numpy())
    print(f"text '{s}': wrapper~st {float((ref_t * lib).sum()):.4f}  tflite~wrapper {float((ref_t * got_t).sum()):.4f}")
    cases.append({"text": s, "ids": tokenize(s)})

# ---------------------------------------------------------------- belirteçleyici
files = st.tokenizer.save_vocabulary(OUT)
os.replace(files[0], os.path.join(OUT, "clip_text_vocab.txt"))
with open(os.path.join(OUT, "clip_tokens_test.json"), "w", encoding="utf-8") as f:
    json.dump({"seq": SEQ, "cases": cases}, f, ensure_ascii=False)
print("bitti:", sorted(os.listdir(OUT)))
