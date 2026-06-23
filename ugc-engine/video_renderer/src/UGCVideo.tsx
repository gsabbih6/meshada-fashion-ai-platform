import { AbsoluteFill, Audio, Img, Sequence, useCurrentFrame, useVideoConfig } from "remotion";
import React from "react";

export const UGCVideo: React.FC<{
  videoUrl: string;
  audioUrl: string;
  captionText: string;
  productImageUrl: string;
  productName: string;
}> = ({ videoUrl, audioUrl, captionText, productImageUrl, productName }) => {
  const frame = useCurrentFrame();
  const { fps, durationInFrames } = useVideoConfig();

  // Simple pop-in animation for the product overlay
  const productScale = Math.min(1, Math.max(0, (frame - 30) / 15));

  return (
    <AbsoluteFill style={{ backgroundColor: "black" }}>
      {/* Background Video (AI Model Lipsync) */}
      <Sequence from={0}>
        <AbsoluteFill>
          <Img
            src={videoUrl}
            style={{
              width: "100%",
              height: "100%",
              objectFit: "cover",
            }}
          />
        </AbsoluteFill>
      </Sequence>

      {/* TTS Audio */}
      <Sequence from={0}>
        <Audio src={audioUrl} />
      </Sequence>

      {/* Product Overlay (appears at 1 second mark) */}
      <Sequence from={30}>
        <AbsoluteFill
          style={{
            justifyContent: "flex-end",
            alignItems: "flex-end",
            padding: "40px",
          }}
        >
          <div
            style={{
              backgroundColor: "rgba(255, 255, 255, 0.9)",
              borderRadius: "20px",
              padding: "20px",
              display: "flex",
              alignItems: "center",
              gap: "20px",
              transform: `scale(${productScale})`,
              boxShadow: "0 10px 30px rgba(0,0,0,0.5)",
            }}
          >
            <Img
              src={productImageUrl}
              style={{
                width: "120px",
                height: "120px",
                borderRadius: "10px",
                objectFit: "cover",
              }}
            />
            <div>
              <h2 style={{ margin: 0, fontFamily: "sans-serif", fontSize: "36px", color: "black" }}>
                {productName}
              </h2>
              <p style={{ margin: 0, fontFamily: "sans-serif", fontSize: "24px", color: "#666" }}>
                Shop Now →
              </p>
            </div>
          </div>
        </AbsoluteFill>
      </Sequence>

      {/* Captions */}
      <Sequence from={0}>
        <AbsoluteFill
          style={{
            justifyContent: "center",
            alignItems: "center",
            paddingTop: "60%",
          }}
        >
          <div
            style={{
              backgroundColor: "rgba(0, 0, 0, 0.6)",
              color: "white",
              padding: "10px 30px",
              borderRadius: "15px",
              fontFamily: "sans-serif",
              fontSize: "48px",
              fontWeight: "bold",
              textAlign: "center",
              maxWidth: "80%",
              textTransform: "uppercase",
            }}
          >
            {captionText}
          </div>
        </AbsoluteFill>
      </Sequence>
    </AbsoluteFill>
  );
};
